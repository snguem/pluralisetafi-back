package pluralisconseil.sn.pluralisEtatFin.exceptions;

/**
 * Thrown when a {@code Societe} on a non-hybride plan with a quota of 1 attempts to register a
 * second {@code Entreprise}. The lock is derived from the société's <em>current</em> active
 * {@code Abonnement.hybride} flag at write time (per the multi-société spec's Global
 * Constraints) rather than stored as a separate flag, so it persists across renewals on another
 * non-hybride plan.
 *
 * <p>Deliberately distinct from {@link EntrepriseQuotaExceededException} so the UI can tell this
 * "locked, no hybride" case apart from an ordinary quota-ceiling rejection.
 */
public class EntrepriseLockedException extends RuntimeException {

    public EntrepriseLockedException(String message) {
        super(message);
    }
}
