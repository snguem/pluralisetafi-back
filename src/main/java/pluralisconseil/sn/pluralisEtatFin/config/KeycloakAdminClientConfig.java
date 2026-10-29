package pluralisconseil.sn.pluralisEtatFin.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminClientConfig {

    @Bean
    public Keycloak keycloakAdminClient(KeycloakProperties properties) {
        var admin = properties.adminService();
        return KeycloakBuilder.builder()
                .serverUrl(admin.serverUrl())
                .realm(properties.realm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(admin.clientId())
                .clientSecret(admin.clientSecret())
                .build();
    }
}
