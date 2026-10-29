package pluralisconseil.sn.pluralisEtatFin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where and how this backend talks to Keycloak. Nothing is hardcoded: the values come from the
 * environment (see {@code application.yml} and {@code .env.example}).
 *
 * @param realm       the realm holding the users and the application client
 * @param clientId    the application client (the one the frontend logs in with): its client roles are the app roles
 * @param adminService the service-account client used to create, block and list users
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(String realm, String clientId, AdminService adminService) {

    /** @param serverUrl the Keycloak base URL as the backend reaches it; {@code clientId}/{@code clientSecret} the service account */
    public record AdminService(String serverUrl, String clientId, String clientSecret) {
    }
}
