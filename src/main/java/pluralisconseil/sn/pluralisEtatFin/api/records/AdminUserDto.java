package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.util.List;

/** A Keycloak account as the platform admin lists it, with the société it belongs to (none for platform admins). */
public record AdminUserDto(
        String id, String username, String email, String firstName, String lastName, boolean enabled,
        List<String> roles, Long societeId, String societeNom) {
}
