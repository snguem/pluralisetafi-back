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
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;
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
        String id = service.createCollaborator(
                "new.collab", "new.collab@example.com", "New", "Collab", "Temp1234!", "COLLABORATEUR");

        assertThat(id).isNotBlank();

        UserRepresentation createdUser = adminClient.realm(REALM).users().get(id).toRepresentation();
        assertThat(createdUser.getUsername()).isEqualTo("new.collab");
        assertThat(createdUser.getEmail()).isEqualTo("new.collab@example.com");
        assertThat(createdUser.getFirstName()).isEqualTo("New");
        assertThat(createdUser.getLastName()).isEqualTo("Collab");

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
        service.createCollaborator("dup.user", "dup@example.com", "Dup", "User", "Temp1234!", "COLLABORATEUR");

        assertThatThrownBy(() -> service.createCollaborator(
                "dup.user", "other@example.com", "Dup", "User", "Temp1234!", "COLLABORATEUR"))
                .isInstanceOf(KeycloakUserAlreadyExistsException.class);
    }

    @Test
    void createCollaborator_whenRoleUnknown_throwsBeforeCreatingUser() {
        assertThatThrownBy(() -> service.createCollaborator(
                "never.created", "never.created@example.com", "Never", "Created", "Temp1234!", "NOT_A_REAL_ROLE"))
                .isInstanceOf(IllegalArgumentException.class);

        List<UserRepresentation> matches = adminClient.realm(REALM).users().search("never.created");
        assertThat(matches).isEmpty();
    }

    @Test
    void listUsers_returnsCreatedUsersWithTheirAssignedRoles() {
        service.createCollaborator(
                "list.collab", "list.collab@example.com", "List", "Collab", "Temp1234!", "COLLABORATEUR");
        service.createCollaborator(
                "list.admin", "list.admin@example.com", "List", "Admin", "Temp1234!", "ADMIN_PLATEFORME");

        List<KeycloakUserService.KeycloakUserSummary> users = service.listUsers();

        assertThat(users)
                .filteredOn(u -> "list.collab".equals(u.username()))
                .singleElement()
                .satisfies(u -> {
                    assertThat(u.email()).isEqualTo("list.collab@example.com");
                    assertThat(u.firstName()).isEqualTo("List");
                    assertThat(u.lastName()).isEqualTo("Collab");
                    assertThat(u.enabled()).isTrue();
                    assertThat(u.roles()).contains("COLLABORATEUR");
                });

        assertThat(users)
                .filteredOn(u -> "list.admin".equals(u.username()))
                .singleElement()
                .satisfies(u -> assertThat(u.roles()).contains("ADMIN_PLATEFORME"));
    }

    @Test
    void listAvailableRoles_returnsEveryPluralEtatfiClientRole() {
        List<String> roles = service.listAvailableRoles();

        assertThat(roles).contains("COLLABORATEUR", "INVITE", "ADMIN_COMPTE", "ADMIN_PLATEFORME");
    }

    @Test
    void copyAccess_givesTargetExactlySourcesRoles() {
        String sourceId = service.createCollaborator(
                "copy.source", "copy.source@example.com", "Copy", "Source", "Temp1234!", "ADMIN_PLATEFORME");
        String targetId = service.createCollaborator(
                "copy.target", "copy.target@example.com", "Copy", "Target", "Temp1234!", "COLLABORATEUR");

        service.copyAccess(sourceId, targetId);

        ClientRepresentation pluralEtatfiClient = adminClient.realm(REALM).clients()
                .findByClientId(CLIENT_ID).get(0);

        List<RoleRepresentation> targetRoles = adminClient.realm(REALM).users().get(targetId)
                .roles().clientLevel(pluralEtatfiClient.getId()).listAll();

        assertThat(targetRoles)
                .extracting(RoleRepresentation::getName)
                .containsExactly("ADMIN_PLATEFORME");
    }

    @Test
    void copyAccess_whenSourceUserUnknown_throwsIllegalArgumentException() {
        String targetId = service.createCollaborator(
                "copy.target2", "copy.target2@example.com", "Copy", "Target2", "Temp1234!", "COLLABORATEUR");

        assertThatThrownBy(() -> service.copyAccess("not-a-real-user-id", targetId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copyAccess_whenTargetUserUnknown_throwsIllegalArgumentException() {
        String sourceId = service.createCollaborator(
                "copy.source2", "copy.source2@example.com", "Copy", "Source2", "Temp1234!", "COLLABORATEUR");

        assertThatThrownBy(() -> service.copyAccess(sourceId, "not-a-real-user-id"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
