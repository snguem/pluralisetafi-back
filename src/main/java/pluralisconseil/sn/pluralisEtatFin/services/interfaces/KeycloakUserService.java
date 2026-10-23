package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;

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
     * @param temporaryPassword  the password communicated to the collaborator out of band
     * @param role               one of the {@code plural_etatfi} client roles (e.g.
     *                           {@code COLLABORATEUR}, {@code INVITE}, {@code ADMIN_COMPTE},
     *                           {@code ADMIN_PLATEFORME})
     * @return the new user's Keycloak id
     * @throws KeycloakUserAlreadyExistsException if a user with this username already exists
     */
    String createCollaborator(String username, String email, String temporaryPassword, String role)
            throws KeycloakUserAlreadyExistsException;
}
