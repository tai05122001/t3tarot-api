package vn.t3tarot.api.identity.emailverification.enumeration;

public enum EmailVerificationOutcome {

    VERIFIED,
    ALREADY_VERIFIED,
    LINK_USED,
    LINK_EXPIRED,
    LINK_INVALID;

    public boolean isSuccessful() {
        return this == VERIFIED || this == ALREADY_VERIFIED;
    }
}
