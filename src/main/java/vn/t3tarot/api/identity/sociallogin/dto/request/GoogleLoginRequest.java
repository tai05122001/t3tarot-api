package vn.t3tarot.api.identity.sociallogin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(@NotBlank @Size(max = 4096) String idToken) {

    @Override
    public String toString() {
        return "GoogleLoginRequest[redacted]";
    }
}
