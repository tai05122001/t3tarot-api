package vn.t3tarot.api.identity.emailverification.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.t3tarot.api.common.random.UuidV7Generator;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;
import vn.t3tarot.api.identity.emailverification.repository.EmailVerificationHotPathRepository;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens.OpaqueToken;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Slf4j
@Component
@RequiredArgsConstructor
class EmailVerificationIssuer {

    private final UuidV7Generator uuidGenerator;
    private final OpaqueTokens opaqueTokens;
    private final EmailVerificationHotPathRepository verifications;
    private final VerificationMailSender mailSender;
    private final IdentityProperties properties;

    void issue(UUID userId, UUID identityId, String email, Instant now) {
        OpaqueToken token = opaqueTokens.generate();
        boolean isInserted = verifications.insertIfNoRecent(
                uuidGenerator.next(),
                userId,
                identityId,
                token.getHash(),
                now,
                now.plus(properties.emailVerificationTtl()),
                now.minus(properties.emailVerificationResendInterval()));
        if (isInserted) {
            sendAfterCommit(email, token.getValue());
        }
    }

    private void sendAfterCommit(String email, String tokenValue) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deliver(email, tokenValue);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliver(email, tokenValue);
            }
        });
    }

    private void deliver(String email, String tokenValue) {
        try {
            mailSender.send(email, tokenValue);
        } catch (RuntimeException ex) {
            log.warn("verification_mail_failed errorType={}", ex.getClass().getSimpleName());
        }
    }
}
