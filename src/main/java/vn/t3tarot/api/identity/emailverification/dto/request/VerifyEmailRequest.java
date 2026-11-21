package vn.t3tarot.api.identity.emailverification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(@NotBlank @Size(max = 128) String token) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[redacted]";
    }
}
