package pluralisconseil.sn.pluralisEtatFin.exceptions;

/**
 * Thrown when a user's société has been deactivated. It extends
 * {@link SocieteMembershipNotFoundException} on purpose: every controller already turns that
 * exception into a 401, so a deactivated société is locked out of the whole API without any
 * per-endpoint change.
 */
public class SocieteInactiveException extends SocieteMembershipNotFoundException {

    public SocieteInactiveException(String message) {
        super(message);
    }
}
