package vn.t3tarot.api.identity.shared.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpaqueTokens {

    private static final int TOKEN_BYTES = 32;
    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom;
    private final String digestAlgorithm;

    @Autowired
    public OpaqueTokens(SecureRandom secureRandom) {
        this(secureRandom, DEFAULT_DIGEST_ALGORITHM);
    }

    OpaqueTokens(SecureRandom secureRandom, String digestAlgorithm) {
        this.secureRandom = secureRandom;
        this.digestAlgorithm = digestAlgorithm;
    }

    public OpaqueToken generate() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return new OpaqueToken(value, hash(value));
    }

    public byte[] hash(String tokenValue) {
        try {
            return MessageDigest.getInstance(digestAlgorithm).digest(tokenValue.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("digest algorithm is not available", ex);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static final class OpaqueToken {

        private final String value;
        private final byte[] hash;

        @Override
        public String toString() {
            return "OpaqueToken[redacted]";
        }
    }
}
