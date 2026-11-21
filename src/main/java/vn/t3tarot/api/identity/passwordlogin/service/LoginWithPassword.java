package vn.t3tarot.api.identity.passwordlogin.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.passwordlogin.dto.request.LoginRequest;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.session.service.SessionFacade;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;
import vn.t3tarot.api.identity.shared.entity.PasswordLogin;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.shared.policy.PasswordPolicy;
import vn.t3tarot.api.identity.shared.repository.PasswordCredentialHotPathRepository;
import vn.t3tarot.api.identity.shared.repository.UserIdentityHotPathRepository;
import vn.t3tarot.api.identity.shared.service.IdentityNormalizer;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Component
@Transactional(noRollbackFor = ApiException.class)
class LoginWithPassword {

    private final VietnamClock clock;
    private final PasswordEncoder passwordEncoder;
    private final PasswordCredentialHotPathRepository credentials;
    private final UserIdentityHotPathRepository identities;
    private final SessionFacade sessions;
    private final IdentityProperties properties;
    private final String dummyPasswordHash;

    LoginWithPassword(
            VietnamClock clock,
            PasswordEncoder passwordEncoder,
            PasswordCredentialHotPathRepository credentials,
            UserIdentityHotPathRepository identities,
            SessionFacade sessions,
            IdentityProperties properties,
            OpaqueTokens opaqueTokens) {
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.credentials = credentials;
        this.identities = identities;
        this.sessions = sessions;
        this.properties = properties;
        this.dummyPasswordHash = passwordEncoder.encode(opaqueTokens.generate().getValue());
    }

    TokenResponse execute(LoginRequest request, ClientInfo clientInfo) {
        Instant now = clock.instant();
        Optional<PasswordLogin> found = credentials.findByUsername(IdentityNormalizer.username(request.username()));
        if (found.isEmpty()) {
            matches(request.password(), dummyPasswordHash);
            throw new ApiException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        PasswordLogin login = found.get();
        boolean isPasswordCorrect = matches(request.password(), login.passwordHash());
        if (login.isLockedAt(now)) {
            throw new ApiException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        if (!isPasswordCorrect) {
            credentials.recordFailure(
                    login.userId(), properties.maxFailedLogins(), now.plus(properties.lockoutDuration()), now);
            throw new ApiException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        if (login.status() != AccountStatus.ACTIVE) {
            throw new ApiException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        if (!login.isEmailVerified()) {
            throw new ApiException(IdentityErrorCode.EMAIL_NOT_VERIFIED);
        }
        credentials.recordSuccess(login.userId(), now);
        identities.recordLogin(login.identityId(), now);
        return sessions.start(login.userId(), login.identityId(), clientInfo, now);
    }

    private boolean matches(String rawPassword, String passwordHash) {
        return PasswordPolicy.isHashable(rawPassword) && passwordEncoder.matches(rawPassword, passwordHash);
    }
}
