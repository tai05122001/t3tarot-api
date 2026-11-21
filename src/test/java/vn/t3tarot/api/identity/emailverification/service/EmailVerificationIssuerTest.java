package vn.t3tarot.api.identity.emailverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import vn.t3tarot.api.common.random.UuidV7Generator;
import vn.t3tarot.api.identity.IdentityTestProperties;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;
import vn.t3tarot.api.identity.emailverification.config.VerificationMailDispatcher;
import vn.t3tarot.api.identity.emailverification.repository.EmailVerificationHotPathRepository;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

class EmailVerificationIssuerTest {

    private static final String EMAIL = "alice@example.test";
    private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private final List<String> sentTokens = new ArrayList<>();
    private final EmailVerificationHotPathRepository repository = mock(EmailVerificationHotPathRepository.class);
    private final UuidV7Generator uuidGenerator = mock(UuidV7Generator.class);
    private boolean isSenderFailing;
    private LogCapture logs;

    @BeforeEach
    void setUp() {
        when(uuidGenerator.next()).thenReturn(UUID.randomUUID());
        when(repository.insertIfNoRecent(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);
        logs = LogCapture.of(EmailVerificationIssuer.class);
    }

    @AfterEach
    void tearDown() {
        logs.close();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private EmailVerificationIssuer issuerWith(VerificationMailDispatcher dispatcher) {
        VerificationMailSender sender = (email, token) -> {
            if (isSenderFailing) {
                throw new IllegalStateException("provider down for " + email + " " + token);
            }
            sentTokens.add(token);
        };
        return new EmailVerificationIssuer(
                uuidGenerator,
                new OpaqueTokens(new SecureRandom()),
                repository,
                sender,
                dispatcher,
                IdentityTestProperties.identity());
    }

    @Test
    void sendsImmediatelyWhenNoTransactionIsActive() {
        issuerWith(VerificationMailDispatcher.inline()).issue(userId, UUID.randomUUID(), EMAIL, NOW);

        assertThat(sentTokens).hasSize(1);
    }

    @Test
    void sendsNothingWhenRecentTokenExists() {
        when(repository.insertIfNoRecent(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);

        issuerWith(VerificationMailDispatcher.inline()).issue(userId, UUID.randomUUID(), EMAIL, NOW);

        assertThat(sentTokens).isEmpty();
    }

    @Test
    void holdsTheMailUntilTheTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        issuerWith(VerificationMailDispatcher.inline()).issue(userId, UUID.randomUUID(), EMAIL, NOW);
        assertThat(sentTokens).isEmpty();

        TransactionSynchronizationUtils.triggerAfterCommit();
        assertThat(sentTokens).hasSize(1);
    }

    @Test
    void dropsTheMailWhenTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        issuerWith(VerificationMailDispatcher.inline()).issue(userId, UUID.randomUUID(), EMAIL, NOW);
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(sentTokens).isEmpty();
    }

    @Test
    void logsOnlyTheErrorTypeWhenTheDispatcherRejects() throws InterruptedException {
        VerificationMailDispatcher fullDispatcher = new VerificationMailDispatcher(
                task -> {
                    throw new TaskRejectedException("queue is full");
                },
                () -> { });

        assertThatCode(() -> issuerWith(fullDispatcher).issue(userId, UUID.randomUUID(), EMAIL, NOW))
                .doesNotThrowAnyException();

        ILoggingEvent event = logs.next();
        assertThat(event.getFormattedMessage())
                .isEqualTo("verification_mail_rejected userId=" + userId + " errorType=TaskRejectedException");
        assertThat(sentTokens).isEmpty();
    }

    @Test
    void logsOnlyTheErrorTypeWhenTheSenderFails() throws InterruptedException {
        isSenderFailing = true;

        assertThatCode(() -> issuerWith(VerificationMailDispatcher.inline()).issue(userId, UUID.randomUUID(), EMAIL, NOW))
                .doesNotThrowAnyException();

        ILoggingEvent event = logs.next();
        assertThat(event.getFormattedMessage())
                .isEqualTo("verification_mail_failed userId=" + userId + " errorType=IllegalStateException");
        assertThat(event.getThrowableProxy()).isNull();
    }
}
