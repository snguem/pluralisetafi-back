package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for {@link MeRestController}: it must read the {@code sub}, {@code
 * preferred_username} and granted authorities straight off the {@link JwtAuthenticationToken}
 * supplied by Spring Security - no DB lookup, no service dependency.
 *
 * <p>The security filter chain is kept active (rather than disabled via {@code
 * addFilters = false}) so that {@code with(authentication(token))} actually propagates the
 * pre-built {@link JwtAuthenticationToken} down to the controller's {@code Authentication}
 * parameter. {@link MinimalSecurityTestConfig} stands in for the real {@code SecurityConfig}: a
 * permit-all chain avoids requiring a {@code JwtDecoder} bean, which the real resource-server
 * config needs only to validate an incoming bearer token - irrelevant here since the
 * authentication is injected directly.
 */
@WebMvcTest(MeRestController.class)
@org.springframework.context.annotation.Import(MeRestControllerTest.MinimalSecurityTestConfig.class)
class MeRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class MinimalSecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            return http.build();
        }
    }

    private static JwtAuthenticationToken collaborateurToken() {
        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .claim("sub", "abc-123")
                .claim("preferred_username", "fatou")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_COLLABORATEUR"));
        return new JwtAuthenticationToken(jwt, authorities);
    }

    @Test
    void me_withAuthenticatedJwt_returnsSubUsernameAndRoles() throws Exception {
        mockMvc.perform(get("/me").with(authentication(collaborateurToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("abc-123"))
                .andExpect(jsonPath("$.username").value("fatou"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_COLLABORATEUR"));
    }
}
