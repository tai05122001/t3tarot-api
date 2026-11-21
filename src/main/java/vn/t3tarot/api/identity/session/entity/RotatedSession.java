package vn.t3tarot.api.identity.session.entity;

import java.util.UUID;

public record RotatedSession(UUID sessionId, UUID userId) {
}
