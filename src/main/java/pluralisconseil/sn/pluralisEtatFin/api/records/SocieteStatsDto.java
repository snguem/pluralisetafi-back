package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.util.List;

/** Figures of the société dashboard. */
public record SocieteStatsDto(
        long entreprises, int entrepriseQuota, long etatsFinanciers, long membres,
        MeSocieteDto.AbonnementInfo abonnement, List<RecentEtatFinancier> derniersEtatsFinanciers) {

    public record RecentEtatFinancier(Long id, String name, int anneeN, String entrepriseNom) {
    }
}
