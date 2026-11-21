package vn.t3tarot.api.identity.emailverification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendEmailVerificationRequest(@NotBlank @Email @Size(max = 320) String email) {

    @Override
    public String toString() {
        return "ResendEmailVerificationRequest[redacted]";
    }
}
