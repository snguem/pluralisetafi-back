package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.AbonnementPlanDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages the {@code AbonnementPlan} catalog (admin-only).
 */
public interface AbonnementPlanService {

    AbonnementPlanDto create(String nom, int entrepriseQuota, boolean hybrideInclus,
                              boolean hybrideDisponibleEnOption, BigDecimal hybrideSupplementPrix,
                              BigDecimal prixParMois, boolean active);

    AbonnementPlanDto get(Long id);

    List<AbonnementPlanDto> getAll();

    /**
     * Withdraws a formula from the public catalog ({@code active=false}) or puts it back. Existing
     * abonnements keep running on it.
     */
    AbonnementPlanDto setActive(Long id, boolean active);
}
