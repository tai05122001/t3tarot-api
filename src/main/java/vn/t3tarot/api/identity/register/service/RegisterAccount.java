package vn.t3tarot.api.identity.register.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import vn.t3tarot.api.common.constant.CommonErrorCode;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.random.UuidV7Generator;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.emailverification.service.EmailVerificationFacade;
import vn.t3tarot.api.identity.register.dto.request.RegisterRequest;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.shared.policy.PasswordPolicy;
import vn.t3tarot.api.identity.shared.repository.AppUserHotPathRepository;
import vn.t3tarot.api.identity.shared.repository.PasswordCredentialHotPathRepository;
import vn.t3tarot.api.identity.shared.repository.UserIdentityHotPathRepository;
import vn.t3tarot.api.identity.shared.service.IdentityNormalizer;

@Component
@RequiredArgsConstructor
@Transactional
class RegisterAccount {

    private final VietnamClock clock;
    private final UuidV7Generator uuidGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AppUserHotPathRepository users;
    private final UserIdentityHotPathRepository identities;
    private final PasswordCredentialHotPathRepository credentials;
    private final EmailVerificationFacade emailVerification;

    UUID execute(RegisterRequest request) {
        if (!PasswordPolicy.isHashable(request.password())) {
            throw new ApiException(CommonErrorCode.INVALID_PARAM);
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Instant now = clock.instant();
        String email = IdentityNormalizer.email(request.email());
        if (identities.existsEmail(email)) {
            return uuidGenerator.next();
        }
        UUID userId = uuidGenerator.next();
        users.insert(userId, null, now);
        UUID identityId = uuidGenerator.next();
        Optional<UUID> insertedIdentity = identities.insertEmailIdentity(identityId, userId, email, now);
        if (insertedIdentity.isEmpty()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return uuidGenerator.next();
        }
        insertCredential(userId, request.username(), passwordHash, now);
        emailVerification.issue(userId, identityId, email, now);
        return userId;
    }

    private void insertCredential(UUID userId, String username, String passwordHash, Instant now) {
        try {
            credentials.insert(userId, username.strip(), IdentityNormalizer.username(username), passwordHash, now);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(IdentityErrorCode.USERNAME_ALREADY_TAKEN, "username already taken", ex);
        }
    }
}
