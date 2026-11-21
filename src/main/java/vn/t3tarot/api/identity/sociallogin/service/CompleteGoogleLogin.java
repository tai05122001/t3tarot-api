package vn.t3tarot.api.identity.sociallogin.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.constant.CommonErrorCode;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.random.UuidV7Generator;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.session.service.SessionFacade;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;
import vn.t3tarot.api.identity.shared.entity.LinkedIdentity;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.shared.enumeration.IdentityProvider;
import vn.t3tarot.api.identity.shared.repository.AppUserHotPathRepository;
import vn.t3tarot.api.identity.shared.repository.UserIdentityHotPathRepository;
import vn.t3tarot.api.identity.sociallogin.dto.response.GoogleLoginResponse;
import vn.t3tarot.api.identity.sociallogin.entity.GoogleProfile;

@Component
@RequiredArgsConstructor
@Transactional
class CompleteGoogleLogin {

    private static final int MAX_DISPLAY_NAME_LENGTH = 80;

    private final VietnamClock clock;
    private final UuidV7Generator uuidGenerator;
    private final AppUserHotPathRepository users;
    private final UserIdentityHotPathRepository identities;
    private final SessionFacade sessions;

    GoogleLoginResponse execute(GoogleProfile profile, ClientInfo clientInfo) {
        Instant now = clock.instant();
        Optional<LinkedIdentity> existing = identities.findByProviderSubject(IdentityProvider.GOOGLE, profile.subject());
        if (existing.isPresent()) {
            return signInExisting(existing.get(), clientInfo, now);
        }
        if (identities.existsEmailOnOtherProvider(profile.email(), IdentityProvider.GOOGLE)) {
            throw new ApiException(IdentityErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        return signUp(profile, clientInfo, now);
    }

    private GoogleLoginResponse signInExisting(LinkedIdentity identity, ClientInfo clientInfo, Instant now) {
        if (identity.status() != AccountStatus.ACTIVE) {
            throw new ApiException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        identities.recordLogin(identity.identityId(), now);
        return GoogleLoginResponse.from(
                sessions.start(identity.userId(), identity.identityId(), clientInfo, now), false);
    }

    private GoogleLoginResponse signUp(GoogleProfile profile, ClientInfo clientInfo, Instant now) {
        UUID userId = uuidGenerator.next();
        UUID identityId = uuidGenerator.next();
        users.insert(userId, displayName(profile.displayName()), now);
        try {
            identities.insertGoogleIdentity(identityId, userId, profile.subject(), profile.email(), now);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(CommonErrorCode.CONFLICT, "concurrent first google login", ex);
        }
        identities.recordLogin(identityId, now);
        return GoogleLoginResponse.from(sessions.start(userId, identityId, clientInfo, now), true);
    }

    private static String displayName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String stripped = name.strip();
        return stripped.length() <= MAX_DISPLAY_NAME_LENGTH ? stripped : stripped.substring(0, MAX_DISPLAY_NAME_LENGTH);
    }
}
