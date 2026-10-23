package pluralisconseil.sn.pluralisEtatFin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the application as an OAuth2 resource server backed by Keycloak: any request to a
 * non-public endpoint must carry a JWT access token issued by the realm configured under
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. Keycloak client roles for the
 * {@code plural_etatfi} client are exposed as {@code ROLE_*} granted authorities via
 * {@link KeycloakClientRoleConverter} for later authorization checks.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String KEYCLOAK_CLIENT_ID = "plural_etatfi";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(customizer -> customizer.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(
                                "/swagger-ui.html/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/users/login",
                                "/json/**",
                                "/config-model-excel/all-model-list",
                                "/api/v1/etats-fin/",
                                "/config-model-excel/set-all/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN_PLATEFORME")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
        ;
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakClientRoleConverter(KEYCLOAK_CLIENT_ID));
        return converter;
    }
}
