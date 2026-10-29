package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.time.LocalDate;
import java.util.List;

/** Figures of the platform admin dashboard. */
public record PlatformStatsDto(
        long societes, long societesActives, long abonnementsEnCours, long abonnementsExpirantSous30Jours,
        long utilisateurs, long entreprises, long etatsFinanciers,
        List<LabelCount> etatsParMois, List<LabelCount> repartitionFormules,
        List<Expiration> prochainesExpirations, List<RecentSociete> dernieresSocietes, Revenus revenus) {

    /** Money earned from the abonnements sold (the day they were activated). */
    public record Revenus(java.math.BigDecimal total, java.math.BigDecimal moisCourant, java.math.BigDecimal moisPrecedent,
                          List<LabelAmount> parMois, List<LabelAmount> parFormule) {
    }


    public record Expiration(Long societeId, String societeNom, String planNom, LocalDate dateFin, long joursRestants) {
    }

    public record RecentSociete(Long id, String nom, boolean active) {
    }
}
