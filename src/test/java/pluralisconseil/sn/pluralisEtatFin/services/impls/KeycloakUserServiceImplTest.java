package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;
import pluralisconseil.sn.pluralisEtatFin.support.KeycloakTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link KeycloakUserServiceImpl} against a real Keycloak instance (the shared
 * Testcontainers container from {@link KeycloakTestSupport}, with Task 1's {@code pluraletatfin}
 * realm already imported) rather than a mocked admin client, so a genuine lack of permissions on
 * {@code service-account-admin-service} (e.g. missing {@code manage-clients}) surfaces as a real
 * test failure instead of being hidden behind a stub.
 *
 * <p>Deliberately not a {@code @SpringBootTest}: the service is constructed directly with an
 * admin {@link Keycloak} client built by hand and pointed at the container's admin REST base URL,
 * matching the {@code admin-service} client-credentials grant this plan's production
 * {@code KeycloakAdminClientConfig} bean also uses.
 */
class KeycloakUserServiceImplTest {

    private static final String REALM = "pluraletatfin";
    private static final String CLIENT_ID = "plural_etatfi";

    private static Keycloak adminClient;
    private static KeycloakUserServiceImpl service;

    @BeforeAll
    static void setUp() {
        String serverUrl = "http://" + KeycloakTestSupport.KEYCLOAK.getHost() + ":"
                + KeycloakTestSupport.KEYCLOAK.getMappedPort(8080);

        adminClient = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(REALM)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId("admin-service")
                .clientSecret("omvtA3yRPWqSPras6Sn7qid6Wa95Din3ZLvPVonRDhLCr2sFHEDCiK8hn5wBdZtJuyVx2ObpbLeUPwxK7ZUkns")
                .build();

        service = new KeycloakUserServiceImpl(adminClient);
    }

    @Test
    void createCollaborator_createsUserWithRoleAssignedInKeycloak() {
        String id = service.createCollaborator("new.collab", "new.collab@example.com", "Temp1234!", "COLLABORATEUR");

        assertThat(id).isNotBlank();

        UserRepresentation createdUser = adminClient.realm(REALM).users().get(id).toRepresentation();
        assertThat(createdUser.getUsername()).isEqualTo("new.collab");
        assertThat(createdUser.getEmail()).isEqualTo("new.collab@example.com");

        ClientRepresentation pluralEtatfiClient = adminClient.realm(REALM).clients()
                .findByClientId(CLIENT_ID).get(0);

        List<RoleRepresentation> assignedClientRoles = adminClient.realm(REALM).users().get(id)
                .roles().clientLevel(pluralEtatfiClient.getId()).listAll();

        assertThat(assignedClientRoles)
                .extracting(RoleRepresentation::getName)
                .contains("COLLABORATEUR");
    }

    @Test
    void createCollaborator_whenUsernameAlreadyExists_throwsKeycloakUserAlreadyExistsException() {
        service.createCollaborator("dup.user", "dup@example.com", "Temp1234!", "COLLABORATEUR");

        assertThatThrownBy(() -> service.createCollaborator("dup.user", "other@example.com", "Temp1234!", "COLLABORATEUR"))
                .isInstanceOf(KeycloakUserAlreadyExistsException.class);
    }
}
