package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces the old DB-backed {@code GET /users/me} (removed along with the rest of the local-auth
 * stack). This endpoint performs no DB lookup: it reads the {@code sub}, {@code
 * preferred_username} and granted authorities directly off the {@link JwtAuthenticationToken}
 * that {@link pluralisconseil.sn.pluralisEtatFin.config.SecurityConfig}'s resource-server chain
 * has already authenticated against Keycloak.
 */
@RestController
@CrossOrigin("*")
public class MeRestController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String sub = authentication.getName();
        String username = sub;

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            String subClaim = jwt.getClaimAsString("sub");
            if (subClaim != null) {
                sub = subClaim;
            }
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null) {
                username = preferredUsername;
            }
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", sub);
        body.put("username", username);
        body.put("roles", roles);
        return body;
    }
}
