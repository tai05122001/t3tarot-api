package vn.t3tarot.api.common.error;

import java.util.regex.Pattern;

public final class ErrorCodeFormat {

    private static final Pattern PATTERN = Pattern.compile("^error(\\.[a-z]+(_[a-z]+)*){2}$");

    private ErrorCodeFormat() {
    }

    public static boolean isValid(String code) {
        return code != null && PATTERN.matcher(code).matches();
    }
}
