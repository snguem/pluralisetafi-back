package pluralisconseil.sn.pluralisEtatFin.exceptions;

/**
 * Thrown when a {@code Societe} attempts to register more {@code Entreprise} than its currently
 * active {@code Abonnement}'s plan allows ({@code AbonnementPlan.entrepriseQuota}), or has no
 * active {@code Abonnement} at all (treated as a quota of zero).
 *
 * <p>Deliberately distinct from {@link EntrepriseLockedException} so the UI can tell a société
 * that simply needs a bigger plan apart from one that is permanently locked to its first
 * {@code Entreprise} by a non-hybride plan.
 */
public class EntrepriseQuotaExceededException extends RuntimeException {

    public EntrepriseQuotaExceededException(String message) {
        super(message);
    }
}
