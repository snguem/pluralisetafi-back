package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EtatFinancierRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.support.KeycloakTestSupport;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Review Focus of Task 6: verifies — against a real Keycloak-issued token, not a mocked JWT —
 * that a {@code COLLABORATEUR} belonging to a different société than the one that owns a given
 * {@code EtatFinancier} cannot export its PDF just by guessing its id.
 *
 * <p>Follows {@code SecurityConfigIntegrationTest}'s pattern (full application context, a
 * disposable MySQL Testcontainer standing in for the dev database, {@link KeycloakTestSupport}'s
 * shared Keycloak Testcontainer for real token issuance) but adds the repositories needed to
 * seed two sociétés, one entreprise, one état financier, and a société membership for the
 * realm's {@code fatou} test user (a fixed Keycloak id, {@code COLLABORATEUR} client role)
 * pointing at the société that does NOT own the état financier under test.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.http.client.factory=simple")
@Testcontainers
class EtatFinancierSocieteScopingIntegrationTest {

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>(DockerImageName.parse("mysql:8.4"))
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "etat_fin_app")
            .withExposedPorts(3306)
            .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 2)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakTestSupport::issuerUri);
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/etat_fin_app");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private SocieteRepository societeRepository;
    @Autowired
    private EntrepriseRepository entrepriseRepository;
    @Autowired
    private EtatFinancierRepository etatFinancierRepository;
    @Autowired
    private UserSocieteRepository userSocieteRepository;

    // The realm's "fatou" test user (src/test/resources/keycloak/realm-export.json): fixed
    // Keycloak id, COLLABORATEUR client role, password "test-password".
    private static final String FATOU_KEYCLOAK_ID = "4012dbf5-4ab7-4b8e-931d-18fa88ec9c7c";

    @Test
    void postPdf_withCollaborateurTokenOfAnotherSociete_returns401() {
        var societeOwner = societeRepository.save(Societe.builder().nom("Owner").code("SCOPE-OWNER-" + System.nanoTime()).active(true).build());
        var societeOfCaller = societeRepository.save(Societe.builder().nom("Caller").code("SCOPE-CALLER-" + System.nanoTime()).active(true).build());

        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise("OwnerCo");
        var entreprise = entrepriseRepository.save(Entreprise.builder()
                .active(true)
                .substitute(substitute)
                .societe(societeOwner)
                .build());

        var etatFinancier = etatFinancierRepository.save(EtatFinancier.builder()
                .active(true)
                .name("EF-scope-test")
                .entreprise(substitute) // embedded snapshot: same entreprise, irrelevant to the check
                .entrepriseRef(entreprise)
                .build());

        // fatou belongs to a DIFFERENT société than the one owning etatFinancier above.
        userSocieteRepository.save(UserSociete.builder()
                .keycloakUserId(FATOU_KEYCLOAK_ID)
                .societeId(societeOfCaller.getId())
                .role("COLLABORATEUR")
                .build());

        String token = KeycloakTestSupport.passwordGrantToken("fatou", "test-password");
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = rest.exchange(
                "/etats-fin/{id}/pdf", HttpMethod.POST,
                new HttpEntity<>(List.<String>of(), headers), byte[].class,
                etatFinancier.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
