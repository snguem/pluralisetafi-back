package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;

import java.util.List;

/**
 * Creates collaborator accounts directly in Keycloak (the {@code pluraletatfin} realm), using the
 * {@code admin-service} client's service account. There is no local user table any more: Keycloak
 * is the single source of truth for accounts and roles.
 */
public interface KeycloakUserService {

    /**
     * Creates an enabled Keycloak user with the given temporary password (not flagged as
     * Keycloak-temporary, so the collaborator is not forced through a hosted "set your password"
     * flow) and assigns the given {@code plural_etatfi} client role to it.
     *
     * @param username           the new user's username
     * @param email              the new user's email
     * @param firstName          the new user's first name (required by Keycloak's User Profile
     *                           validation for the user to be able to log in)
     * @param lastName           the new user's last name (required by Keycloak's User Profile
     *                           validation for the user to be able to log in)
     * @param temporaryPassword  the password communicated to the collaborator out of band
     * @param role               one of the {@code plural_etatfi} client roles (e.g.
     *                           {@code COLLABORATEUR}, {@code INVITE}, {@code ADMIN_COMPTE},
     *                           {@code ADMIN_PLATEFORME})
     * @return the new user's Keycloak id
     * @throws KeycloakUserAlreadyExistsException if a user with this username already exists
     */
    String createCollaborator(
            String username, String email, String firstName, String lastName, String temporaryPassword, String role)
            throws KeycloakUserAlreadyExistsException;

    /**
     * Lists every user in the {@code pluraletatfin} realm along with the {@code plural_etatfi}
     * client role(s) (e.g. {@code COLLABORATEUR}, {@code ADMIN_PLATEFORME}) assigned to each,
     * so a platform admin can see every account in the system. No pagination: this realm is
     * expected to have a small number of users.
     *
     * @return a summary of every realm user
     */
    List<KeycloakUserSummary> listUsers();

    /**
     * Lists every {@code plural_etatfi} client role name that exists (e.g. {@code COLLABORATEUR},
     * {@code INVITE}, {@code ADMIN_COMPTE}, {@code ADMIN_PLATEFORME}), for a UI to show as
     * assignable options.
     *
     * @return every assignable role name
     */
    List<String> listAvailableRoles();

    /**
     * Makes {@code targetUserId}'s {@code plural_etatfi} client roles exactly match
     * {@code sourceUserId}'s current roles: after this call, the target has the same access as
     * the source, no more and no less (the target's own prior roles are removed, not merged with
     * the source's).
     *
     * @param sourceUserId the Keycloak id of the user whose access is copied
     * @param targetUserId the Keycloak id of the user who receives that access
     * @throws IllegalArgumentException if either user does not exist in Keycloak
     */
    void copyAccess(String sourceUserId, String targetUserId);

    /**
     * Blocks ({@code enabled=false}) or unblocks ({@code enabled=true}) a Keycloak account. Blocking
     * also ends the user's open Keycloak sessions, so they cannot refresh their tokens.
     *
     * @throws IllegalArgumentException if the user does not exist in Keycloak
     */
    void setUserEnabled(String userId, boolean enabled);

    /**
     * A read-only summary of a Keycloak user, for listing purposes.
     *
     * @param id        the user's Keycloak id
     * @param username  the user's username
     * @param email     the user's email
     * @param firstName the user's first name
     * @param lastName  the user's last name
     * @param enabled   whether the user is enabled
     * @param roles     the user's {@code plural_etatfi} client role names (raw Keycloak role
     *                  names, e.g. {@code COLLABORATEUR}, not prefixed with {@code ROLE_})
     */
    record KeycloakUserSummary(
            String id, String username, String email, String firstName, String lastName, boolean enabled,
            List<String> roles) {
    }
}
