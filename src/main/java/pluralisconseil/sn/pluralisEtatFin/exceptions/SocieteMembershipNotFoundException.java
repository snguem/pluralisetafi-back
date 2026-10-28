package pluralisconseil.sn.pluralisEtatFin.exceptions;

/**
 * Thrown when a Keycloak user has no corresponding {@code UserSociete} membership row, so no
 * société/role can be resolved for them.
 */
public class SocieteMembershipNotFoundException extends RuntimeException {

    public SocieteMembershipNotFoundException(String message) {
        super(message);
    }
}
