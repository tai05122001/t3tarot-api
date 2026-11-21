package vn.t3tarot.api.identity.register.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.t3tarot.api.identity.shared.policy.PasswordPolicy;

public record RegisterRequest(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9_.]{3,32}$") String username,
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_BYTES) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + "]";
    }
}
