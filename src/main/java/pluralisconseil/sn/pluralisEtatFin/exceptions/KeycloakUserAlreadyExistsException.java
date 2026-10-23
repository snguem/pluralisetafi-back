package pluralisconseil.sn.pluralisEtatFin.exceptions;

/**
 * Thrown when creating a Keycloak user fails because a user with the same username (or email,
 * depending on the realm's uniqueness settings) already exists in the realm.
 */
public class KeycloakUserAlreadyExistsException extends RuntimeException {

    public KeycloakUserAlreadyExistsException(String message) {
        super(message);
    }
}
