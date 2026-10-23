package pluralisconseil.sn.pluralisEtatFin.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Reusable Testcontainers support for any test that needs a real Keycloak instance with the
 * {@code pluraletatfin} realm (from {@code src/test/resources/keycloak/realm-export.json},
 * produced by Task 1) imported and ready to issue tokens.
 *
 * <p>This follows Testcontainers' documented "singleton container" pattern: a single
 * {@code static} container field, started once via a static initializer, and reused across
 * every test class in the same JVM test run (Ryuk reaps it when the JVM exits). Deliberately
 * NOT a {@code @Testcontainers}/{@code @Container} instance field, which would start a fresh
 * container per test class instead of sharing one.
 *
 * <p>Both {@link #issuerUri()} (for a test's {@code @DynamicPropertySource}) and
 * {@link #passwordGrantToken(String, String)} (to obtain a bearer token for one of the realm's
 * test users) are intended to be reused as-is by any later test needing the same Keycloak
 * fixture, so this logic lives in exactly one place.
 */
public class KeycloakTestSupport {

    private static final String REALM = "pluraletatfin";
    private static final String CLIENT_ID = "plural_etatfi";
    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.8.0";

    public static final GenericContainer<?> KEYCLOAK;

    static {
        KEYCLOAK = new GenericContainer<>(KEYCLOAK_IMAGE)
                .withExposedPorts(8080)
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                .withClasspathResourceMapping(
                        "keycloak/realm-export.json",
                        "/opt/keycloak/data/import/realm-export.json",
                        BindMode.READ_ONLY)
                .withCommand("start-dev", "--import-realm")
                .waitingFor(Wait.forHttp("/realms/" + REALM)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        KEYCLOAK.start();
    }

    private KeycloakTestSupport() {
    }

    /**
     * The issuer URI for the imported {@code pluraletatfin} realm, resolved against the
     * container's mapped port. Register this under
     * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} via
     * {@code @DynamicPropertySource}.
     */
    public static String issuerUri() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/" + REALM;
    }

    /**
     * Obtains an access token for one of the realm's test users via the OAuth2 "password"
     * grant against {@code plural_etatfi} (which has direct access grants enabled for this
     * purpose in the dev/test realm export only).
     *
     * @param username the test user's username (e.g. {@code "fatou"})
     * @param password the test user's password (e.g. {@code "test-password"})
     * @return the raw access token string
     */
    public static String passwordGrantToken(String username, String password) {
        String tokenUrl = issuerUri() + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", CLIENT_ID);
        body.add("username", username);
        body.add("password", password);

        ResponseEntity<String> response = new RestTemplate()
                .postForEntity(tokenUrl, new HttpEntity<>(body, headers), String.class);

        try {
            JsonNode json = new ObjectMapper().readTree(response.getBody());
            return json.get("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not parse access_token from Keycloak token response: " + response.getBody(), e);
        }
    }
}
