package vn.t3tarot.api.common.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public final class PemKeySource {

    private PemKeySource() {
    }

    public static String read(String inlineKey, Resource keyLocation, String keyName) {
        boolean hasInlineKey = StringUtils.hasText(inlineKey);
        boolean hasKeyLocation = keyLocation != null;
        if (hasInlineKey && hasKeyLocation) {
            throw new IllegalStateException(keyName + " is set both inline and by location; set only one");
        }
        if (hasInlineKey) {
            return inlineKey;
        }
        if (hasKeyLocation) {
            return readLocation(keyLocation, keyName);
        }
        throw new IllegalStateException(keyName + " is missing; set it inline or by location");
    }

    private static String readLocation(Resource keyLocation, String keyName) {
        try {
            return keyLocation.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(keyName + " file is not readable: " + keyLocation.getDescription(), ex);
        }
    }
}
