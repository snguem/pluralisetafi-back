package pluralisconseil.sn.pluralisEtatFin.api.records;

/** Figures of the platform admin dashboard. */
public record PlatformStatsDto(
        long societes, long societesActives, long abonnementsEnCours, long abonnementsExpirantSous30Jours,
        long utilisateurs, long entreprises, long etatsFinanciers) {
}
