package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.time.LocalDate;

/**
 * Who the signed-in user is inside their société: used by the frontend shell (name of the société,
 * role, read-only banner when the abonnement is over).
 */
public record MeSocieteDto(
        Long societeId, String societeNom, String role, boolean readOnly, AbonnementInfo abonnement) {

    /** The société's active abonnement; {@code current} is false once it is over or before it starts. */
    public record AbonnementInfo(
            String planNom, LocalDate dateDebut, LocalDate dateFin, boolean hybride, boolean current) {
    }
}
