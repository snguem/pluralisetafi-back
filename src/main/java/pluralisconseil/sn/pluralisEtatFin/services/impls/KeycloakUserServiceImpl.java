package pluralisconseil.sn.pluralisEtatFin.services.impls;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;

import java.util.List;

/**
 * Creates collaborator accounts in the {@code pluraletatfin} Keycloak realm via the
 * {@code admin-service} client's service account, and assigns them a {@code plural_etatfi}
 * client role.
 *
 * <p>The {@code plural_etatfi} client's UUID is never hardcoded: it is resolved at runtime via
 * {@link RealmResource#clients()}{@code .findByClientId(...)} on every call, since Keycloak
 * assigns that UUID when the realm is created/imported and it is not known ahead of time.
 */
@Service
@RequiredArgsConstructor
public class KeycloakUserServiceImpl implements KeycloakUserService {

    private static final String REALM = "pluraletatfin";
    private static final String PLURAL_ETATFI_CLIENT_ID = "plural_etatfi";

    private final Keycloak keycloakAdminClient;

    @Override
    public String createCollaborator(
            String username, String email, String firstName, String lastName, String temporaryPassword, String role)
            throws KeycloakUserAlreadyExistsException {
        RealmResource realmResource = keycloakAdminClient.realm(REALM);

        // Resolve and validate the client role BEFORE creating the Keycloak user: if `role`
        // doesn't match any of plural_etatfi's client roles, fail here so no orphaned,
        // roleless account is left behind (a retry after that would then hit 409 on the
        // username, hiding the orphan).
        ClientRepresentation pluralEtatfiClient = realmResource.clients()
                .findByClientId(PLURAL_ETATFI_CLIENT_ID)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Keycloak client '" + PLURAL_ETATFI_CLIENT_ID + "' not found in realm '" + REALM + "'"));

        RoleRepresentation roleRepresentation;
        try {
            roleRepresentation = realmResource.clients()
                    .get(pluralEtatfiClient.getId())
                    .roles()
                    .get(role)
                    .toRepresentation();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Unknown '" + PLURAL_ETATFI_CLIENT_ID + "' client role: '" + role + "'", ex);
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(temporaryPassword);
        // Not Keycloak-temporary: per this plan's "no email, admin communicates the password
        // directly" decision, the collaborator must not be forced through a Keycloak-hosted
        // "set your password" flow they'd need an email to reach.
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        String userId;
        try (Response response = realmResource.users().create(user)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new KeycloakUserAlreadyExistsException(
                        "A Keycloak user named '" + username + "' already exists");
            }
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new IllegalStateException(
                        "Failed to create Keycloak user '" + username + "', status=" + response.getStatus());
            }
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        // Defense-in-depth: the role was already validated above, so this should not normally
        // fail. If it still does (e.g. a transient Keycloak error), delete the just-created user
        // rather than leaving a stray, roleless account behind.
        try {
            realmResource.users().get(userId)
                    .roles()
                    .clientLevel(pluralEtatfiClient.getId())
                    .add(List.of(roleRepresentation));
        } catch (RuntimeException ex) {
            realmResource.users().get(userId).remove();
            throw ex;
        }

        return userId;
    }
}
