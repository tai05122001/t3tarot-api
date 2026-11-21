package vn.t3tarot.api.identity.session.dto.response;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static final String BEARER = "Bearer";

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, BEARER, expiresIn);
    }

    @Override
    public String toString() {
        return "TokenResponse[expiresIn=" + expiresIn + "]";
    }
}
