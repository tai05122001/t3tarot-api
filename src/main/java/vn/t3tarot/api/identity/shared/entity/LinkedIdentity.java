package vn.t3tarot.api.identity.shared.entity;

import java.util.UUID;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;

public record LinkedIdentity(UUID userId, UUID identityId, AccountStatus status) {
}
