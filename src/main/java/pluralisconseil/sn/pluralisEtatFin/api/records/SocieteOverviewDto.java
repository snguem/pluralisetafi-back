package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.time.LocalDate;

/** A société as the platform admin lists it: its state, current abonnement and size. */
public record SocieteOverviewDto(
        Long id, String nom, boolean active, String planNom, LocalDate dateFin,
        boolean abonnementCurrent, long entreprises, long membres) {
}
