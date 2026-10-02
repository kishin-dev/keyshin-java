package lol.kishin.keyshin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Client for the KeyShin validation API ({@code POST /api/v1/validate}).
 *
 * <pre>{@code
 * boolean ok = KeyShinClient.validate("https://licenses.kishin.lol", "DBOT-VNHG-QX4Z-LEXA-ZJ74", "discord-bot-pro");
 * }</pre>
 *
 * The {@code url} may be the server's base URL or the full validate endpoint.
 * All calls are blocking, so don't run them on a UI / game / main server thread.
 */
public final class KeyShinClient {

    private static final String VALIDATE_PATH = "/api/v1/validate";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private KeyShinClient() {
    }

    /**
     * Checks a license key. Returns {@code true} only when the server answers that the key is valid.
     * Any failure (invalid key, server down, timeout, bad response) returns {@code false}.
     * Use {@link #check} if you need to tell "invalid" apart from "couldn't check".
     */
    public static boolean validate(String url, String license, String product) {
        return validate(url, license, product, null, null);
    }

    /**
     * Same as {@link #validate(String, String, String)}, but also registers this machine
     * as an activation of the license.
     *
     * @param fingerprint stable id for this machine (e.g. a hash of the machine id), or null
     * @param label       readable name shown in the dashboard (e.g. hostname), or null
     */
    public static boolean validate(String url, String license, String product, String fingerprint, String label) {
        try {
            return check(url, license, product, fingerprint, label).valid();
        } catch (KeyShinException e) {
            return false;
        }
    }

    /** Full result of a check, including the reason code and the customer-facing message. */
    public static ValidationResult check(String url, String license, String product) throws KeyShinException {
        return check(url, license, product, null, null);
    }

    /**
     * Full result of a check, optionally registering this machine as an activation.
     *
     * @throws KeyShinException if the check itself failed (network error, non-200 status, unreadable response).
     *                          This means "couldn't check", not "invalid license".
     */
    public static ValidationResult check(String url, String license, String product,
                                         String fingerprint, String label) throws KeyShinException {
        URI endpoint = endpoint(requireText(url, "url"));
        requireText(license, "license");
        requireText(product, "product");

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "keyshin-java")
                .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody(license, product, fingerprint, label), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new KeyShinException("Could not reach the license server: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyShinException("Interrupted while checking the license", e);
        }

        int status = response.statusCode();
        if (status != 200) {
            throw new KeyShinException("License server returned HTTP " + status, status, serverError(response.body()));
        }

        Map<String, Object> json;
        try {
            json = Json.parseObject(response.body());
        } catch (IllegalArgumentException e) {
            throw new KeyShinException("License server sent an unreadable response", e);
        }
        return ValidationResult.from(json);
    }

    static URI endpoint(String url) {
        String base = url.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.endsWith(VALIDATE_PATH)) {
            base += VALIDATE_PATH;
        }
        URI uri = URI.create(base);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("url must start with http:// or https://");
        }
        return uri;
    }

    static String requestBody(String license, String product, String fingerprint, String label) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"key\":").append(Json.quote(license));
        sb.append(",\"product\":").append(Json.quote(product));
        if (fingerprint != null && !fingerprint.isBlank()) {
            sb.append(",\"fingerprint\":").append(Json.quote(fingerprint));
        }
        if (label != null && !label.isBlank()) {
            sb.append(",\"label\":").append(Json.quote(label));
        }
        return sb.append('}').toString();
    }

    private static String serverError(String body) {
        try {
            Object error = Json.parseObject(body).get("error");
            return error instanceof String ? (String) error : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }
}
