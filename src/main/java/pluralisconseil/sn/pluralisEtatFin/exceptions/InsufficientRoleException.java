package pluralisconseil.sn.pluralisEtatFin.exceptions;

/**
 * Thrown when a caller's {@code UserSociete} role does not have enough rank in the
 * {@code ADMIN_PLATEFORME} &gt; {@code ADMIN_COMPTE} &gt; {@code COLLABORATEUR}/{@code INVITE}
 * hierarchy to perform the requested société-membership operation (e.g. an {@code ADMIN_COMPTE}
 * trying to create another {@code ADMIN_COMPTE}).
 */
public class InsufficientRoleException extends RuntimeException {

    public InsufficientRoleException(String message) {
        super(message);
    }
}
