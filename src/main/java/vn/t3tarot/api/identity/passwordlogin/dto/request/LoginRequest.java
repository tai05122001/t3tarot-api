package vn.t3tarot.api.identity.passwordlogin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.t3tarot.api.identity.shared.policy.PasswordPolicy;

public record LoginRequest(
        @NotBlank @Size(max = 32) String username,
        @NotBlank @Size(max = PasswordPolicy.MAX_BYTES) String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + "]";
    }
}
