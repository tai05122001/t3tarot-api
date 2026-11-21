package vn.t3tarot.api.identity;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

public class CapturingVerificationMailSender implements VerificationMailSender {

    private final Map<String, Deque<String>> tokensByEmail = new ConcurrentHashMap<>();
    private volatile boolean isFailing;

    @Override
    public void send(String email, String verificationToken) {
        if (isFailing) {
            throw new IllegalStateException("mail provider is down");
        }
        tokensByEmail.computeIfAbsent(email, key -> new ConcurrentLinkedDeque<>()).add(verificationToken);
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
}
