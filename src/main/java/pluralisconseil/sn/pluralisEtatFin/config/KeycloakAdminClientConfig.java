package pluralisconseil.sn.pluralisEtatFin.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the {@code admin-service} client's Keycloak admin REST client, authenticated via the
 * OAuth2 {@code client_credentials} grant against the {@code pluraletatfin} realm. Used by
 * {@link pluralisconseil.sn.pluralisEtatFin.services.impls.KeycloakUserServiceImpl} to create and
 * manage collaborator accounts.
 */
@Configuration
public class KeycloakAdminClientConfig {

    private static final String REALM = "pluraletatfin";
    private static final String CLIENT_ID = "admin-service";

    @Bean
    public Keycloak keycloakAdminClient(
            @Value("${keycloak.admin-service.server-url}") String serverUrl,
            @Value("${keycloak.admin-service.client-secret}") String clientSecret) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(REALM)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(CLIENT_ID)
                .clientSecret(clientSecret)
                .build();
    }
}
