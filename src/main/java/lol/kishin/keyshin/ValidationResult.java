package lol.kishin.keyshin;

import java.util.Map;

/**
 * The server's answer to a validation request.
 *
 * @param valid          true only if the key works for this product (and machine)
 * @param code           valid, not_found, wrong_product, revoked, expired or activation_limit
 * @param message        customer-facing explanation, safe to show to the user
 * @param product        product slug, may be null
 * @param expiresAt      ISO-8601 expiry timestamp, or null if the license never expires
 * @param maxActivations machine limit, or null if not reported
 * @param activations    machines currently using the license, or null if not reported
 * @param newActivation  true if this request registered a new machine
 */
public record ValidationResult(
        boolean valid,
        String code,
        String message,
        String product,
        String expiresAt,
        Integer maxActivations,
        Integer activations,
        boolean newActivation) {

    public static final String VALID = "valid";
    public static final String NOT_FOUND = "not_found";
    public static final String WRONG_PRODUCT = "wrong_product";
    public static final String REVOKED = "revoked";
    public static final String EXPIRED = "expired";
    public static final String ACTIVATION_LIMIT = "activation_limit";

    static ValidationResult from(Map<String, Object> json) {
        return new ValidationResult(
                Boolean.TRUE.equals(json.get("valid")), // fail closed: anything but literal true is invalid
                string(json.get("code")),
                string(json.get("message")),
                string(json.get("product")),
                string(json.get("expiresAt")),
                integer(json.get("maxActivations")),
                integer(json.get("activations")),
                Boolean.TRUE.equals(json.get("newActivation")));
    }

    private static String string(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static Integer integer(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }
}
