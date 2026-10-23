package pluralisconseil.sn.pluralisEtatFin.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extracts the Keycloak client roles for one specific client from a JWT's
 * {@code resource_access} claim and exposes them as Spring Security
 * {@link GrantedAuthority GrantedAuthorities}, prefixed with {@code ROLE_}.
 */
public class KeycloakClientRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;

    public KeycloakClientRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return List.of();
        }

        Object clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
            return List.of();
        }

        Object roles = clientAccessMap.get("roles");
        if (!(roles instanceof List<?> rolesList)) {
            return List.of();
        }

        return rolesList.stream()
                .filter(Objects::nonNull)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
