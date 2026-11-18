package vn.t3tarot.api.common.error;

import java.util.Map;

public final class ViolationCodes {

    public static final String IS_MISSING = "error.invalid_param.is_missing";
    public static final String IS_INVALID = "error.invalid_param.is_invalid";
    public static final String INVALID_FORMAT = "error.invalid_param.invalid_format";
    public static final String INVALID_TYPE = "error.invalid_param.invalid_type";
    public static final String OUT_OF_RANGE = "error.invalid_param.out_of_range";
    public static final String LENGTH_OUT_OF_RANGE = "error.invalid_param.length_out_of_range";

    private static final Map<String, String> BY_CONSTRAINT = Map.ofEntries(
            Map.entry("NotNull", IS_MISSING),
            Map.entry("NotBlank", IS_MISSING),
            Map.entry("NotEmpty", IS_MISSING),
            Map.entry("Size", LENGTH_OUT_OF_RANGE),
            Map.entry("Length", LENGTH_OUT_OF_RANGE),
            Map.entry("Min", OUT_OF_RANGE),
            Map.entry("Max", OUT_OF_RANGE),
            Map.entry("DecimalMin", OUT_OF_RANGE),
            Map.entry("DecimalMax", OUT_OF_RANGE),
            Map.entry("Range", OUT_OF_RANGE),
            Map.entry("Positive", OUT_OF_RANGE),
            Map.entry("PositiveOrZero", OUT_OF_RANGE),
            Map.entry("Negative", OUT_OF_RANGE),
            Map.entry("NegativeOrZero", OUT_OF_RANGE),
            Map.entry("Pattern", INVALID_FORMAT),
            Map.entry("Email", INVALID_FORMAT),
            Map.entry("typeMismatch", INVALID_TYPE));

    private ViolationCodes() {
    }

    public static String of(String constraintName) {
        if (constraintName == null) {
            return IS_INVALID;
        }
        return BY_CONSTRAINT.getOrDefault(constraintName, IS_INVALID);
    }
}
