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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.SocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.models.SocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.support.KeycloakTestSupport;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link SocieteServiceImpl} and {@link UserSocieteServiceImpl} against real
 * infrastructure: a real {@code Societe}/{@code UserSociete} persistence layer (a disposable
 * MySQL Testcontainer, local to this test class, stood up via the {@code @DataJpaTest} slice
 * with {@code replace = NONE} so it is not swapped for an embedded database) and a real
 * {@link KeycloakUserServiceImpl} built, like {@link KeycloakUserServiceImplTest}, from a hand-
 * built admin {@link Keycloak} client pointed at the shared {@link KeycloakTestSupport#KEYCLOAK}
 * singleton container (no second Keycloak container is started).
 *
 * <p>The {@code @DataJpaTest} slice only loads JPA infrastructure (entities, Spring Data
 * repositories), not {@code @Service} beans or {@code SecurityConfig}, so
 * {@link SocieteServiceImpl} and {@link UserSocieteServiceImpl} are constructed by hand from the
 * slice's real, Spring-Data-backed repositories plus MapStruct-generated mapper impls and the
 * hand-built {@link KeycloakUserServiceImpl} — mirroring the "do not mock Keycloak" approach
 * {@link KeycloakUserServiceImplTest} already uses for the Keycloak side.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SocieteServiceImplTest {

    private static final String REALM = "pluraletatfin";

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
    private SocieteRepository societeRepository;
    @Autowired
    private UserSocieteRepository userSocieteRepository;

    private SocieteServiceImpl societeService;
    private UserSocieteServiceImpl userSocieteService;

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
        userSocieteService = new UserSocieteServiceImpl(userSocieteRepository, new UserSocieteMapperImpl());
        societeService = new SocieteServiceImpl(
                societeRepository, new SocieteMapperImpl(), keycloakUserService, userSocieteService);
    }

    @Test
    void create_createsSocieteAndAttachesAdminAsUserSociete() {
        var dto = societeService.create("Cabinet Fatou", "CAB-001",
                "admin.cabinet", "admin.cabinet@cabinet.sn", "Admin", "Cabinet", "Temp1234!");

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getNom()).isEqualTo("Cabinet Fatou");

        // SocieteService.create(...) does not expose the Keycloak id it generated for the new
        // admin, so look it up in Keycloak the same way a real caller inspecting the realm would.
        String newUserId = adminClient.realm(REALM).users().search("admin.cabinet").get(0).getId();

        var membership = userSocieteService.resolveByKeycloakUserId(newUserId);
        assertThat(membership.getSocieteId()).isEqualTo(dto.getId());
        assertThat(membership.getRole()).isEqualTo("ADMIN_COMPTE");
    }

    @Test
    void get_returnsThePreviouslyCreatedSociete() {
        var created = societeService.create("Cabinet Nafi", "CAB-002",
                "admin.nafi", "admin.nafi@cabinet.sn", "Admin", "Nafi", "Temp1234!");

        var fetched = societeService.get(created.getId());

        assertThat(fetched.getNom()).isEqualTo("Cabinet Nafi");
        assertThat(fetched.getCode()).isEqualTo("CAB-002");
    }

    @Test
    void getAll_filtersByNomSearchParam() {
        societeService.create("Cabinet Awa", "CAB-003",
                "admin.awa", "admin.awa@cabinet.sn", "Admin", "Awa", "Temp1234!");
        societeService.create("Cabinet Modou", "CAB-004",
                "admin.modou", "admin.modou@cabinet.sn", "Admin", "Modou", "Temp1234!");

        var page = societeService.getAll(Map.of("nom", "Awa"), PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(SocieteDto::getNom)
                .containsExactly("Cabinet Awa");
    }
}
