package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.AbonnementDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;

import java.time.LocalDate;

/**
 * Activates and resolves a {@code Societe}'s {@code Abonnement} to an {@code AbonnementPlan}.
 *
 * <p>{@code Abonnement.hybride} is copied from {@code AbonnementPlan.hybrideInclus} at
 * activation time, unless the plan has {@code hybrideDisponibleEnOption = true}, in which case
 * the caller's explicit {@code hybrideChoisi} is used instead. {@code hybrideChoisi} is ignored
 * when the plan doesn't offer the option, and must be {@code true} when the plan both offers
 * the option and already includes hybride by default (not actually optional in that case).
 */
public interface AbonnementService {

    AbonnementDto activate(Long societeId, Long planId, int nbMois, LocalDate dateDebut, boolean hybrideChoisi);

    /**
     * Returns the société's currently active {@code Abonnement} entity (not DTO), for internal
     * use by other services (Task 4/5), or {@code null} if none is active.
     */
    Abonnement getActiveEntity(Long societeId);

    /**
     * Whether the société's active {@code Abonnement} covers today ({@code dateDebut} and
     * {@code dateFin} included). {@code false} when there is none, it has expired or it has not
     * started yet: the société is then read-only.
     */
    boolean isCurrent(Long societeId);
}
