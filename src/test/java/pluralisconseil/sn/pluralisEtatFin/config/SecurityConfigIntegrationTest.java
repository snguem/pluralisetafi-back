package pluralisconseil.sn.pluralisEtatFin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.support.KeycloakTestSupport;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SecurityConfig} genuinely behaves as an OAuth2 resource server against a
 * real Keycloak instance (via {@link KeycloakTestSupport}'s shared Testcontainers container
 * importing Task 1's {@code pluraletatfin} realm) rather than against a mocked JWT decoder.
 *
 * <p>This test boots the full application context, which also requires a relational database
 * (JPA/Hibernate). A disposable MySQL container (local to this test class, not shared - unlike
 * {@link KeycloakTestSupport}'s Keycloak container) stands in for the dev MySQL instance so the
 * context can start without any external database being available.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // Forces the classic java.net.HttpURLConnection-based client instead of the JDK
        // java.net.http.HttpClient one that Spring Boot would otherwise auto-detect: the
        // latter's loopback-connection optimisation (Unix domain sockets) is broken in this
        // sandboxed environment, failing every TestRestTemplate call with
        // "Unable to establish loopback connection" before a request is even sent.
        properties = "spring.http.client.factory=simple")
@Testcontainers
class SecurityConfigIntegrationTest {

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>(DockerImageName.parse("mysql:8.4"))
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "etat_fin_app")
            .withExposedPorts(3306)
            .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 2)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void issuerUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakTestSupport::issuerUri);
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/etat_fin_app");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private TestRestTemplate rest;

    // TestRestTemplate's root URI already incorporates server.servlet.context-path (/api/v1),
    // so the path used here must be relative to that context root, not repeat it.
    private static final String ETATS_FIN_ALL_PATH = "/etats-fin/all";

    @Test
    void requestWithNoToken_onAnAuthenticatedEndpoint_returns401() {
        var response = rest.getForEntity(ETATS_FIN_ALL_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requestWithValidTokenForCollaborateurUser_returns200() {
        String token = KeycloakTestSupport.passwordGrantToken("fatou", "test-password"); // COLLABORATEUR

        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange(ETATS_FIN_ALL_PATH, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requestWithInvalidToken_returns401() {
        var headers = new HttpHeaders();
        headers.setBearerAuth("not-a-real-jwt-at-all");
        var response = rest.exchange(ETATS_FIN_ALL_PATH, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // TestRestTemplate's root URI already incorporates server.servlet.context-path (/api/v1).
    private static final String ADMIN_USERS_PATH = "/admin/users";

    @Test
    void postAdminUsers_withNonAdminPlateformeToken_returns403() {
        String token = KeycloakTestSupport.passwordGrantToken("fatou", "test-password"); // COLLABORATEUR

        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        var body = """
                {"username":"should.not.be.created","email":"should.not.be.created@example.com",
                 "firstName":"Should","lastName":"NotBeCreated","temporaryPassword":"Temp1234!","role":"COLLABORATEUR"}
                """;
        var response = rest.exchange(ADMIN_USERS_PATH, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void postAdminUsers_withAdminPlateformeToken_isNotForbidden() {
        String token = KeycloakTestSupport.passwordGrantToken("awa", "test-password"); // ADMIN_PLATEFORME

        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        var body = """
                {"username":"admin.created.user","email":"admin.created.user@example.com",
                 "firstName":"Admin","lastName":"Created","temporaryPassword":"Temp1234!","role":"COLLABORATEUR"}
                """;
        var response = rest.exchange(ADMIN_USERS_PATH, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }
}
