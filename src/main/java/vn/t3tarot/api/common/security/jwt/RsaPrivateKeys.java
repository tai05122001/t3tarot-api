package vn.t3tarot.api.common.security.jwt;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

public final class RsaPrivateKeys {

    public static final int MIN_MODULUS_BITS = 2048;

    private static final Pattern PEM_ARMOR_ESCAPED_NEWLINE_OR_WHITESPACE =
            Pattern.compile("-----(?:BEGIN|END) PRIVATE KEY-----|\\\\n|\\s");

    private RsaPrivateKeys() {
    }

    public static RSAPrivateKey parse(String encodedKey) {
        byte[] derBytes = decode(encodedKey);
        PrivateKey privateKey = generate(derBytes);
        if (!(privateKey instanceof RSAPrivateKey rsaPrivateKey)) {
            throw new IllegalArgumentException("JWT private key must be an RSA key");
        }
        if (rsaPrivateKey.getModulus().bitLength() < MIN_MODULUS_BITS) {
            throw new IllegalArgumentException("JWT private key must be at least " + MIN_MODULUS_BITS + " bits");
        }
        return rsaPrivateKey;
    }

    private static byte[] decode(String encodedKey) {
        String base64Body = PEM_ARMOR_ESCAPED_NEWLINE_OR_WHITESPACE.matcher(encodedKey).replaceAll("");
        try {
            return Base64.getDecoder().decode(base64Body);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT private key is not valid Base64", ex);
        }
    }

    private static PrivateKey generate(byte[] derBytes) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(derBytes));
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("JWT private key is not a valid PKCS#8 RSA private key", ex);
        }
    }
}
