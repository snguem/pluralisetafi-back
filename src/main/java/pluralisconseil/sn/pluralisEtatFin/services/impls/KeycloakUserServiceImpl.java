package pluralisconseil.sn.pluralisEtatFin.services.impls;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import pluralisconseil.sn.pluralisEtatFin.config.KeycloakProperties;
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
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService.KeycloakUserSummary;

import java.util.List;
import java.util.stream.Collectors;

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

    private final Keycloak keycloakAdminClient;
    private final KeycloakProperties properties;

    @Override
    public String createCollaborator(
            String username, String email, String firstName, String lastName, String temporaryPassword, String role)
            throws KeycloakUserAlreadyExistsException {
        RealmResource realmResource = keycloakAdminClient.realm(properties.realm());

        // Resolve and validate the client role BEFORE creating the Keycloak user: if `role`
        // doesn't match any of plural_etatfi's client roles, fail here so no orphaned,
        // roleless account is left behind (a retry after that would then hit 409 on the
        // username, hiding the orphan).
        ClientRepresentation pluralEtatfiClient = resolvePluralEtatfiClient(realmResource);

        RoleRepresentation roleRepresentation;
        try {
            roleRepresentation = realmResource.clients()
                    .get(pluralEtatfiClient.getId())
                    .roles()
                    .get(role)
                    .toRepresentation();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Unknown '" + properties.clientId() + "' client role: '" + role + "'", ex);
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

    @Override
    public List<KeycloakUserSummary> listUsers() {
        RealmResource realmResource = keycloakAdminClient.realm(properties.realm());
        ClientRepresentation pluralEtatfiClient = resolvePluralEtatfiClient(realmResource);

        return realmResource.users().list().stream()
                .map(user -> {
                    List<String> roles = realmResource.users().get(user.getId())
                            .roles()
                            .clientLevel(pluralEtatfiClient.getId())
                            .listAll()
                            .stream()
                            .map(RoleRepresentation::getName)
                            .collect(Collectors.toList());

                    return new KeycloakUserSummary(
                            user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                            user.getLastName(), Boolean.TRUE.equals(user.isEnabled()), roles);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listAvailableRoles() {
        RealmResource realmResource = keycloakAdminClient.realm(properties.realm());
        ClientRepresentation pluralEtatfiClient = resolvePluralEtatfiClient(realmResource);

        return realmResource.clients()
                .get(pluralEtatfiClient.getId())
                .roles()
                .list()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void copyAccess(String sourceUserId, String targetUserId) {
        RealmResource realmResource = keycloakAdminClient.realm(properties.realm());
        ClientRepresentation pluralEtatfiClient = resolvePluralEtatfiClient(realmResource);

        // Validate both users exist before touching any role assignment, so a typo'd id fails
        // loudly instead of silently no-oping.
        requireUserExists(realmResource, sourceUserId);
        requireUserExists(realmResource, targetUserId);

        List<RoleRepresentation> sourceRoles = realmResource.users().get(sourceUserId)
                .roles()
                .clientLevel(pluralEtatfiClient.getId())
                .listAll();

        List<RoleRepresentation> targetCurrentRoles = realmResource.users().get(targetUserId)
                .roles()
                .clientLevel(pluralEtatfiClient.getId())
                .listAll();

        if (!targetCurrentRoles.isEmpty()) {
            realmResource.users().get(targetUserId)
                    .roles()
                    .clientLevel(pluralEtatfiClient.getId())
                    .remove(targetCurrentRoles);
        }

        if (!sourceRoles.isEmpty()) {
            realmResource.users().get(targetUserId)
                    .roles()
                    .clientLevel(pluralEtatfiClient.getId())
                    .add(sourceRoles);
        }
    }

    @Override
    public java.util.Optional<KeycloakUserSummary> findUser(String userId) {
        try {
            UserRepresentation user = keycloakAdminClient.realm(properties.realm()).users().get(userId).toRepresentation();
            return java.util.Optional.of(new KeycloakUserSummary(
                    user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    Boolean.TRUE.equals(user.isEnabled()), List.of()));
        } catch (jakarta.ws.rs.NotFoundException ex) {
            return java.util.Optional.empty();
        }
    }

    @Override
    public void setUserEnabled(String userId, boolean enabled) {
        RealmResource realmResource = keycloakAdminClient.realm(properties.realm());
        requireUserExists(realmResource, userId);

        UserRepresentation user = realmResource.users().get(userId).toRepresentation();
        user.setEnabled(enabled);
        realmResource.users().get(userId).update(user);
        if (!enabled) {
            realmResource.users().get(userId).logout();
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the given user id does not exist in the realm.
     * The Keycloak admin client surfaces a missing user as a 404-shaped runtime exception when
     * fetching its representation, so that is used as the existence check.
     */
    private void requireUserExists(RealmResource realmResource, String userId) {
        try {
            realmResource.users().get(userId).toRepresentation();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown Keycloak user id: '" + userId + "'", ex);
        }
    }

    /**
     * Resolves the {@code plural_etatfi} client's UUID, which is never hardcoded since Keycloak
     * assigns it when the realm is created/imported.
     */
    private ClientRepresentation resolvePluralEtatfiClient(RealmResource realmResource) {
        return realmResource.clients()
                .findByClientId(properties.clientId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Keycloak client '" + properties.clientId() + "' not found in realm '" + properties.realm() + "'"));
    }
}
