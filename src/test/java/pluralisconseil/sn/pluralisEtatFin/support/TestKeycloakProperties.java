package pluralisconseil.sn.pluralisEtatFin.support;

import pluralisconseil.sn.pluralisEtatFin.config.KeycloakProperties;

/** Keycloak settings of the test realm (src/test/resources/keycloak/realm-export.json). */
public final class TestKeycloakProperties {

    public static final KeycloakProperties VALUE = new KeycloakProperties(
            "pluraletatfin", "plural_etatfi",
            new KeycloakProperties.AdminService("http://localhost:8080", "admin-service", "test-secret"));

    private TestKeycloakProperties() {
    }
}
