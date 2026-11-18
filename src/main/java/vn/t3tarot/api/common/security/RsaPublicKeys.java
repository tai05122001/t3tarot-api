package vn.t3tarot.api.common.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

public final class RsaPublicKeys {

    public static final int MIN_MODULUS_BITS = 2048;

    private static final Pattern PEM_ARMOR_ESCAPED_NEWLINE_OR_WHITESPACE =
            Pattern.compile("-----(?:BEGIN|END) PUBLIC KEY-----|\\\\n|\\s");

    private RsaPublicKeys() {
    }

    public static RSAPublicKey parse(String encodedKey) {
        byte[] derBytes = decode(encodedKey);
        PublicKey publicKey = generate(derBytes);
        if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
            throw new IllegalArgumentException("JWT public key must be an RSA key");
        }
        if (rsaPublicKey.getModulus().bitLength() < MIN_MODULUS_BITS) {
            throw new IllegalArgumentException("JWT public key must be at least " + MIN_MODULUS_BITS + " bits");
        }
        return rsaPublicKey;
    }

    private static byte[] decode(String encodedKey) {
        String base64Body = PEM_ARMOR_ESCAPED_NEWLINE_OR_WHITESPACE.matcher(encodedKey).replaceAll("");
        try {
            return Base64.getDecoder().decode(base64Body);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT public key is not valid Base64", ex);
        }
    }

    private static PublicKey generate(byte[] derBytes) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(derBytes));
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("JWT public key is not a valid X.509 RSA public key", ex);
        }
    }
}
