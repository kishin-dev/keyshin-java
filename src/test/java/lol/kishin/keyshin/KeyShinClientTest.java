package lol.kishin.keyshin;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class KeyShinClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/validate", ex -> {
            lastBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, out.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void validLicense() {
        reply = "{\"valid\":true,\"code\":\"valid\",\"message\":\"License is valid.\",\"product\":\"discord-bot-pro\","
                + "\"expiresAt\":null,\"maxActivations\":2,\"activations\":1,\"newActivation\":true}";
        assertTrue(KeyShinClient.validate(baseUrl, "DBOT-VNHG-QX4Z-LEXA-ZJ74", "discord-bot-pro"));
        assertEquals("{\"key\":\"DBOT-VNHG-QX4Z-LEXA-ZJ74\",\"product\":\"discord-bot-pro\"}", lastBody.get());
    }

    @Test
    void invalidLicenseHasCodeAndMessage() throws Exception {
        reply = "{\"valid\":false,\"code\":\"activation_limit\",\"message\":\"Too many machines.\",\"maxActivations\":2,\"activations\":2}";
        assertFalse(KeyShinClient.validate(baseUrl, "X", "p"));
        ValidationResult r = KeyShinClient.check(baseUrl, "X", "p", "fp-123", "server-1");
        assertEquals(ValidationResult.ACTIVATION_LIMIT, r.code());
        assertEquals(2, r.activations());
        assertTrue(lastBody.get().contains("\"fingerprint\":\"fp-123\""));
    }

    @Test
    void fullEndpointUrlAlsoWorks() {
        reply = "{\"valid\":true}";
        assertTrue(KeyShinClient.validate(baseUrl + "/api/v1/validate/", "X", "p"));
    }

    @Test
    void serverErrorIsFalseButCheckThrows() {
        status = 502;
        reply = "{\"error\":\"database unavailable\"}";
        assertFalse(KeyShinClient.validate(baseUrl, "X", "p"));
        KeyShinException e = assertThrows(KeyShinException.class, () -> KeyShinClient.check(baseUrl, "X", "p"));
        assertEquals(502, e.getStatusCode());
        assertEquals("database unavailable", e.getServerError());
    }

    @Test
    void garbageResponseIsFalse() {
        reply = "<html>not json</html>";
        assertFalse(KeyShinClient.validate(baseUrl, "X", "p"));
    }

    @Test
    void unreachableServerIsFalse() {
        server.stop(0);
        assertFalse(KeyShinClient.validate(baseUrl, "X", "p"));
    }

    @Test
    void escapesSpecialCharacters() {
        reply = "{\"valid\":false}";
        KeyShinClient.validate(baseUrl, "a\"b\\c", "p");
        assertEquals("{\"key\":\"a\\\"b\\\\c\",\"product\":\"p\"}", lastBody.get());
    }
}
