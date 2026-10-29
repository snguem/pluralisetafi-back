package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.AbonnementDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AbonnementServiceImpl implements AbonnementService {

    private final AbonnementRepository repository;
    private final AbonnementPlanRepository planRepository;
    private final SocieteRepository societeRepository;
    private final AbonnementMapper mapper;

    @Override
    public AbonnementDto activate(Long societeId, Long planId, int nbMois, LocalDate dateDebut,
                                   boolean hybrideChoisi) {
        var societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new RuntimeException("Societe not found: " + societeId));
        if (Boolean.FALSE.equals(societe.getActive())) {
            throw new SocieteInactiveException("La société " + societeId + " est désactivée");
        }
        AbonnementPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("AbonnementPlan not found: " + planId));

        boolean hybride = resolveHybride(plan, hybrideChoisi);

        // At most one Abonnement is expected to be active per société at a time (see
        // AbonnementRepository#findBySocieteIdAndActiveTrue), so deactivate any currently active
        // one for this société before activating the new one.
        Abonnement currentlyActive = repository.findBySocieteIdAndActiveTrue(societeId);
        if (currentlyActive != null) {
            currentlyActive.setActive(false);
            repository.save(currentlyActive);
        }

        Abonnement entity = Abonnement.builder()
                .societeId(societeId)
                .planId(planId)
                .nbMois(nbMois)
                .hybride(hybride)
                .dateDebut(dateDebut)
                .dateFin(dateDebut.plusMonths(nbMois))
                .active(true)
                .build();

        return mapper.asDto(repository.save(entity));
    }

    private boolean resolveHybride(AbonnementPlan plan, boolean hybrideChoisi) {
        if (plan.isHybrideDisponibleEnOption()) {
            if (plan.isHybrideInclus() && !hybrideChoisi) {
                throw new IllegalArgumentException(
                        "hybrideChoisi must be true: plan '" + plan.getNom() + "' already includes hybride");
            }
            return hybrideChoisi;
        }
        return plan.isHybrideInclus();
    }

    @Override
    public Abonnement getActiveEntity(Long societeId) {
        return repository.findBySocieteIdAndActiveTrue(societeId);
    }

    @Override
    public boolean isCurrent(Long societeId) {
        Abonnement abonnement = repository.findBySocieteIdAndActiveTrue(societeId);
        if (abonnement == null) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(abonnement.getDateDebut()) && !today.isAfter(abonnement.getDateFin());
    }
}
