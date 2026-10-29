package pluralisconseil.sn.pluralisEtatFin.api.records;

/** A member of a société as the admin sees them: who they are (from Keycloak) and their role in the société. */
public record SocieteMemberDto(
        Long id, String keycloakUserId, String username, String email, String firstName, String lastName,
        String role, boolean enabled) {
}
