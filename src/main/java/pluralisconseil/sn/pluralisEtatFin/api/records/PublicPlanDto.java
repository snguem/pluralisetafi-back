package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.math.BigDecimal;

/** An abonnement formula as shown to anyone on the public landing page. */
public record PublicPlanDto(
        Long id, String nom, int entrepriseQuota, BigDecimal prixParMois,
        boolean hybrideInclus, boolean hybrideDisponibleEnOption, BigDecimal hybrideSupplementPrix) {
}
