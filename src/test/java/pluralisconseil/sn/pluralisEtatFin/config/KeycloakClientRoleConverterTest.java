package pluralisconseil.sn.pluralisEtatFin.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakClientRoleConverterTest {

    private final KeycloakClientRoleConverter converter = new KeycloakClientRoleConverter("plural_etatfi");

    @Test
    void convert_extractsAndPrefixesRolesForTheConfiguredClient() {
        Jwt jwt = jwtWithResourceAccess(Map.of(
            "plural_etatfi", Map.of("roles", List.of("COLLABORATEUR", "ADMIN_COMPTE"))
        ));
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_COLLABORATEUR", "ROLE_ADMIN_COMPTE");
    }

    @Test
    void convert_ignoresRolesOfOtherClients() {
        Jwt jwt = jwtWithResourceAccess(Map.of(
            "some-other-client", Map.of("roles", List.of("SOMETHING")),
            "plural_etatfi", Map.of("roles", List.of("INVITE"))
        ));
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_INVITE");
    }

    @Test
    void convert_returnsEmptyWhenResourceAccessHasNoEntryForTheConfiguredClient() {
        Jwt jwt = jwtWithResourceAccess(Map.of("some-other-client", Map.of("roles", List.of("X"))));
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void convert_returnsEmptyWhenResourceAccessClaimIsAbsent() {
        Jwt jwt = jwtWithResourceAccess(null); // builder simply omits the claim
        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt jwtWithResourceAccess(Map<String, Object> resourceAccess) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test");
        if (resourceAccess != null) {
            builder.claim("resource_access", resourceAccess);
        }
        return builder.build();
    }
}
