package pluralisconseil.sn.pluralisEtatFin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(KeycloakProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .exceptionHandling(customizer -> customizer.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(
                                "/swagger-ui.html/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // landing page: open to everyone, no token
                        .requestMatchers(HttpMethod.GET, "/public/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN_PLATEFORME")
                        // models and their configuration are global, shared by every société: read-only for them
                        .requestMatchers(HttpMethod.POST, "/models/**", "/config-model-excel/**").hasRole("ADMIN_PLATEFORME")
                        .requestMatchers(HttpMethod.PUT, "/models/**", "/config-model-excel/**").hasRole("ADMIN_PLATEFORME")
                        .requestMatchers(HttpMethod.DELETE, "/models/**", "/config-model-excel/**").hasRole("ADMIN_PLATEFORME")
                        .requestMatchers("/societe/**").hasAnyRole("ADMIN_COMPTE", "ADMIN_PLATEFORME")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)))
        ;
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakProperties keycloakProperties) {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakClientRoleConverter(keycloakProperties.clientId()));
        return converter;
    }
}
