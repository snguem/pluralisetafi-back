package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.MeSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.PlatformStatsDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.PublicPlanDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.SocieteStatsDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.*;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DashboardService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AbonnementPlanRepository planRepository;
    private final AbonnementRepository abonnementRepository;
    private final AbonnementService abonnementService;
    private final SocieteRepository societeRepository;
    private final UserSocieteRepository userSocieteRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EtatFinancierRepository etatFinancierRepository;

    @Override
    public List<PublicPlanDto> publicPlans() {
        return planRepository.findByActiveTrueOrderByPrixParMoisAsc().stream()
                .map(plan -> new PublicPlanDto(plan.getId(), plan.getNom(), plan.getEntrepriseQuota(),
                        plan.getPrixParMois(), plan.isHybrideInclus(), plan.isHybrideDisponibleEnOption(),
                        plan.getHybrideSupplementPrix()))
                .toList();
    }

    @Override
    public MeSocieteDto meSociete(UserSocieteDto caller) {
        var societe = societeRepository.findById(caller.getSocieteId())
                .orElseThrow(() -> new RuntimeException("Societe not found: " + caller.getSocieteId()));
        return new MeSocieteDto(societe.getId(), societe.getNom(), caller.getRole(), caller.isReadOnly(),
                abonnementInfo(caller.getSocieteId()));
    }

    @Override
    public SocieteStatsDto societeStats(UserSocieteDto caller) {
        Long societeId = caller.getSocieteId();
        var abonnement = abonnementService.getActiveEntity(societeId);
        int quota = abonnement == null ? 0 : planRepository.findById(abonnement.getPlanId())
                .map(plan -> plan.getEntrepriseQuota()).orElse(0);

        var recent = etatFinancierRepository.findTop5ByEntrepriseRef_Societe_IdOrderByIdDesc(societeId).stream()
                .map(e -> new SocieteStatsDto.RecentEtatFinancier(e.getId(), e.getName(), e.getAnnee_n(),
                        e.getEntrepriseRef() == null || e.getEntrepriseRef().getSubstitute() == null
                                ? null : e.getEntrepriseRef().getSubstitute().getNameEntreprise()))
                .toList();

        return new SocieteStatsDto(
                entrepriseRepository.countBySociete_Id(societeId), quota,
                etatFinancierRepository.countByEntrepriseRef_Societe_Id(societeId),
                userSocieteRepository.countBySocieteId(societeId),
                abonnementInfo(societeId), recent);
    }

    @Override
    public PlatformStatsDto platformStats() {
        LocalDate today = LocalDate.now();
        return new PlatformStatsDto(
                societeRepository.count(), societeRepository.countByActiveTrue(),
                abonnementRepository.countByActiveTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(today, today),
                abonnementRepository.countByActiveTrueAndDateFinBetween(today, today.plusDays(30)),
                userSocieteRepository.count(), entrepriseRepository.count(), etatFinancierRepository.count());
    }

    private MeSocieteDto.AbonnementInfo abonnementInfo(Long societeId) {
        Abonnement abonnement = abonnementService.getActiveEntity(societeId);
        if (abonnement == null) return null;
        String planNom = planRepository.findById(abonnement.getPlanId()).map(plan -> plan.getNom()).orElse(null);
        return new MeSocieteDto.AbonnementInfo(planNom, abonnement.getDateDebut(), abonnement.getDateFin(),
                abonnement.isHybride(), abonnementService.isCurrent(societeId));
    }
}
