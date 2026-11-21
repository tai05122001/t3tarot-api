package vn.t3tarot.api.identity;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import vn.t3tarot.api.common.web.RequestIdFilter;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

public class CapturingVerificationMailSender implements VerificationMailSender {

    private static final long GATE_TIMEOUT_SECONDS = 10;

    private final Map<String, Deque<String>> tokensByEmail = new ConcurrentHashMap<>();
    private volatile boolean isFailing;
    private volatile CountDownLatch gate = new CountDownLatch(0);
    private volatile CountDownLatch attempts = new CountDownLatch(0);
    private volatile String lastAttemptedToken;
    private volatile String lastRequestIdSeen;
    private volatile String lastThreadNameSeen;

    @Override
    public void send(String email, String verificationToken) {
        try {
            lastAttemptedToken = verificationToken;
            lastRequestIdSeen = MDC.get(RequestIdFilter.MDC_KEY);
            lastThreadNameSeen = Thread.currentThread().getName();
            awaitGate();
            if (isFailing) {
                throw new IllegalStateException("mail provider is down for " + email + " token " + verificationToken);
            }
            tokensByEmail.computeIfAbsent(email, key -> new ConcurrentLinkedDeque<>()).add(verificationToken);
        } finally {
            attempts.countDown();
        }
    }

    private void awaitGate() {
        try {
            gate.await(GATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public Optional<String> lastTokenOf(String email) {
        Deque<String> tokens = tokensByEmail.get(email);
        return tokens == null ? Optional.empty() : Optional.ofNullable(tokens.peekLast());
    }

    public int sentCountOf(String email) {
        Deque<String> tokens = tokensByEmail.get(email);
        return tokens == null ? 0 : tokens.size();
    }

    public void failWhileSending(boolean shouldFail) {
        this.isFailing = shouldFail;
    }

    public CountDownLatch holdSending() {
        CountDownLatch holding = new CountDownLatch(1);
        this.gate = holding;
        return holding;
    }

    public void releaseSending() {
        this.gate.countDown();
        this.gate = new CountDownLatch(0);
    }

    public CountDownLatch expectAttempts(int count) {
        CountDownLatch expected = new CountDownLatch(count);
        this.attempts = expected;
        return expected;
    }

    public String lastAttemptedToken() {
        return lastAttemptedToken;
    }

    public String lastRequestIdSeen() {
        return lastRequestIdSeen;
    }

    public String lastThreadNameSeen() {
        return lastThreadNameSeen;
    }
}
