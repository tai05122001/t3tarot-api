package vn.t3tarot.api.identity.session.entity;

import java.time.Instant;
import java.util.UUID;

public record ActiveSession(UUID sessionId, Instant createdAt, Instant expiresAt, String userAgent, boolean isCurrent) {
}
