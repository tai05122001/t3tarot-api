package vn.t3tarot.api.identity.sociallogin.dto.response;

import vn.t3tarot.api.identity.session.dto.response.TokenResponse;

public record GoogleLoginResponse(
        String accessToken, String refreshToken, String tokenType, long expiresIn, boolean isNewUser) {

    public static GoogleLoginResponse from(TokenResponse tokens, boolean isNewUser) {
        return new GoogleLoginResponse(
                tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(), tokens.expiresIn(), isNewUser);
    }

    @Override
    public String toString() {
        return "GoogleLoginResponse[expiresIn=" + expiresIn + ", isNewUser=" + isNewUser + "]";
    }
}
