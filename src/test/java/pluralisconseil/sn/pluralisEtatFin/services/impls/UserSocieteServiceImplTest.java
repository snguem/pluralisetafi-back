package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.InsufficientRoleException;
import pluralisconseil.sn.pluralisEtatFin.support.KeycloakTestSupport;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link UserSocieteServiceImpl#createMember} against real infrastructure: a real
 * {@code UserSociete} persistence layer (a disposable MySQL Testcontainer, local to this test
 * class, stood up via the {@code @DataJpaTest} slice with {@code replace = NONE}) and a real
 * {@link KeycloakUserServiceImpl} built, like {@link KeycloakUserServiceImplTest} and
 * {@link SocieteServiceImplTest}, from a hand-built admin {@link Keycloak} client pointed at the
 * shared {@link KeycloakTestSupport#KEYCLOAK} singleton container (no second Keycloak container
 * is started).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserSocieteServiceImplTest {

    private static final String REALM = "pluraletatfin";
    private static final Long ADMIN_COMPTE_SOCIETE_ID = 1L;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>(DockerImageName.parse("mysql:8.4"))
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "etat_fin_app")
            .withExposedPorts(3306)
            .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 2)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/etat_fin_app");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "test");
    }

    private static Keycloak adminClient;
    private static KeycloakUserServiceImpl keycloakUserService;

    @Autowired
    private UserSocieteRepository userSocieteRepository;

    @Autowired
    private SocieteRepository societeRepository;

    private UserSocieteServiceImpl userSocieteService;

    private String adminCompteKeycloakId;

    @BeforeAll
    static void setUpKeycloak() {
        String serverUrl = "http://" + KeycloakTestSupport.KEYCLOAK.getHost() + ":"
                + KeycloakTestSupport.KEYCLOAK.getMappedPort(8080);

        adminClient = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(REALM)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId("admin-service")
                .clientSecret("omvtA3yRPWqSPras6Sn7qid6Wa95Din3ZLvPVonRDhLCr2sFHEDCiK8hn5wBdZtJuyVx2ObpbLeUPwxK7ZUkns")
                .build();

        keycloakUserService = new KeycloakUserServiceImpl(adminClient);
    }

    @BeforeEach
    void setUp() {
        userSocieteService = new UserSocieteServiceImpl(
                userSocieteRepository, new UserSocieteMapperImpl(), keycloakUserService, societeRepository);

        // Seed an ADMIN_COMPTE test user the same way KeycloakUserServiceImplTest seeds users,
        // then attach them to a société so createMember has a caller to resolve.
        adminCompteKeycloakId = keycloakUserService.createCollaborator(
                "admin.compte." + System.nanoTime(), "admin.compte." + System.nanoTime() + "@cabinet.sn",
                "Admin", "Compte", "Temp1234!", "ADMIN_COMPTE");
        userSocieteService.attach(adminCompteKeycloakId, ADMIN_COMPTE_SOCIETE_ID, "ADMIN_COMPTE");
    }

    @Test
    void createMember_whenCallerIsAdminCompte_canCreateCollaborateur() {
        var dto = userSocieteService.createMember(adminCompteKeycloakId, "nouveau.collab." + System.nanoTime(),
                "nc" + System.nanoTime() + "@x.sn", "Nouveau", "Collab", "Temp1234!", "COLLABORATEUR");

        assertThat(dto.getRole()).isEqualTo("COLLABORATEUR");
        assertThat(dto.getSocieteId()).isEqualTo(ADMIN_COMPTE_SOCIETE_ID);
    }

    @Test
    void createMember_whenCallerIsAdminCompte_cannotCreateAdminCompte() {
        assertThatThrownBy(() -> userSocieteService.createMember(adminCompteKeycloakId,
                "x" + System.nanoTime(), "x" + System.nanoTime() + "@x.sn", "X", "X", "Temp1234!", "ADMIN_COMPTE"))
                .isInstanceOf(InsufficientRoleException.class);
    }

    @Test
    void createMember_whenCallerIsAdminCompte_cannotCreateAdminPlateforme() {
        assertThatThrownBy(() -> userSocieteService.createMember(adminCompteKeycloakId,
                "y" + System.nanoTime(), "y" + System.nanoTime() + "@x.sn", "Y", "Y", "Temp1234!",
                "ADMIN_PLATEFORME"))
                .isInstanceOf(InsufficientRoleException.class);
    }

    @Test
    void createMember_whenCallerIsNotAdminCompte_throwsInsufficientRoleException() {
        String collaborateurId = keycloakUserService.createCollaborator(
                "collab.caller." + System.nanoTime(), "collab.caller." + System.nanoTime() + "@x.sn",
                "Collab", "Caller", "Temp1234!", "COLLABORATEUR");
        userSocieteService.attach(collaborateurId, ADMIN_COMPTE_SOCIETE_ID, "COLLABORATEUR");

        assertThatThrownBy(() -> userSocieteService.createMember(collaborateurId,
                "z" + System.nanoTime(), "z" + System.nanoTime() + "@x.sn", "Z", "Z", "Temp1234!", "INVITE"))
                .isInstanceOf(InsufficientRoleException.class);
    }
}
