package pluralisconseil.sn.pluralisEtatFin.api.records;

/**
 * Request body for {@code POST /societe/users}: creates a new Keycloak-backed collaborator
 * attached to the caller's own société, with a role the caller's {@code ADMIN_COMPTE} rank is
 * allowed to grant (see {@code UserSocieteService#createMember}).
 */
public record CreateSocieteUserRequest(
        String username, String email, String firstName, String lastName, String temporaryPassword, String role) {
}
