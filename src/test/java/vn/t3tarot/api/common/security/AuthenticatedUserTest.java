package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthenticatedUserTest {

    @Test
    void parsesCanonicalUuidSubject() {
        assertThat(AuthenticatedUser.parse(TestJwtFactory.USER_A))
                .contains(new AuthenticatedUser(UUID.fromString(TestJwtFactory.USER_A)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "user-1", "3f2a9c1e7b4d4e8a9c2f1a2b3c4d5e6f", "3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6g", "1-1-1-1-1"})
    void returnsEmptyWhenSubjectIsNotCanonicalUuid(String subject) {
        assertThat(AuthenticatedUser.parse(subject)).isEmpty();
    }

    @Test
    void rejectsNullUserId() {
        assertThatThrownBy(() -> new AuthenticatedUser(null)).isInstanceOf(NullPointerException.class);
    }
}
