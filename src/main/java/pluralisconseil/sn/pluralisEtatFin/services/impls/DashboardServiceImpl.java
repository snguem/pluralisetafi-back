package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.AdminUserDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.LabelCount;
import pluralisconseil.sn.pluralisEtatFin.api.records.MeSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.PlatformStatsDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.PublicPlanDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.SocieteOverviewDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.SocieteStatsDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.*;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DashboardService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;

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
    private final KeycloakUserService keycloakUserService;

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

        var etats = etatFinancierRepository.findByEntrepriseRef_Societe_Id(societeId);

        return new SocieteStatsDto(
                entrepriseRepository.countBySociete_Id(societeId), quota,
                etatFinancierRepository.countByEntrepriseRef_Societe_Id(societeId),
                userSocieteRepository.countBySocieteId(societeId),
                abonnementInfo(societeId), recent, parAnnee(etats), parEntreprise(etats));
    }

    @Override
    public PlatformStatsDto platformStats() {
        LocalDate today = LocalDate.now();
        var nomBySociete = new java.util.HashMap<Long, String>();
        societeRepository.findAll().forEach(societe -> nomBySociete.put(societe.getId(), societe.getNom()));
        var nomByPlan = new java.util.HashMap<Long, String>();
        planRepository.findAll().forEach(plan -> nomByPlan.put(plan.getId(), plan.getNom()));

        var current = abonnementRepository.findByActiveTrue().stream()
                .filter(a -> !today.isBefore(a.getDateDebut()) && !today.isAfter(a.getDateFin()))
                .toList();

        var formules = new java.util.TreeMap<String, Long>();
        current.forEach(a -> formules.merge(nomByPlan.getOrDefault(a.getPlanId(), "Formule"), 1L, Long::sum));

        var expirations = current.stream()
                .filter(a -> !a.getDateFin().isAfter(today.plusDays(30)))
                .sorted(java.util.Comparator.comparing(Abonnement::getDateFin))
                .limit(5)
                .map(a -> new PlatformStatsDto.Expiration(a.getSocieteId(), nomBySociete.get(a.getSocieteId()),
                        nomByPlan.get(a.getPlanId()), a.getDateFin(),
                        java.time.temporal.ChronoUnit.DAYS.between(today, a.getDateFin())))
                .toList();

        var dernieres = societeRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 5,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")))
                .getContent().stream()
                .map(s -> new PlatformStatsDto.RecentSociete(s.getId(), s.getNom(), s.getCode(), !Boolean.FALSE.equals(s.getActive())))
                .toList();

        return new PlatformStatsDto(
                societeRepository.count(), societeRepository.countByActiveTrue(),
                abonnementRepository.countByActiveTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(today, today),
                abonnementRepository.countByActiveTrueAndDateFinBetween(today, today.plusDays(30)),
                userSocieteRepository.count(), entrepriseRepository.count(), etatFinancierRepository.count(),
                etatsParMois(today), formules.entrySet().stream().map(e -> new LabelCount(e.getKey(), e.getValue())).toList(),
                expirations, dernieres);
    }

    /** Last 6 years that have états financiers, oldest first. */
    private static List<LabelCount> parAnnee(List<EtatFinancier> etats) {
        var parAnnee = new java.util.TreeMap<Integer, Long>();
        etats.forEach(e -> parAnnee.merge(e.getAnnee_n(), 1L, Long::sum));
        return parAnnee.entrySet().stream()
                .skip(Math.max(0, parAnnee.size() - 6))
                .map(e -> new LabelCount(String.valueOf(e.getKey()), e.getValue()))
                .toList();
    }

    /** The 4 entreprises with the most états financiers, the others grouped under "Autres". */
    private static List<LabelCount> parEntreprise(List<EtatFinancier> etats) {
        var parNom = new java.util.HashMap<String, Long>();
        etats.forEach(e -> parNom.merge(
                e.getEntrepriseRef() == null || e.getEntrepriseRef().getSubstitute() == null
                        || e.getEntrepriseRef().getSubstitute().getNameEntreprise() == null
                        ? "Sans nom" : e.getEntrepriseRef().getSubstitute().getNameEntreprise(), 1L, Long::sum));
        var sorted = parNom.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(java.util.Map.Entry.comparingByKey()))
                .toList();
        var result = new java.util.ArrayList<LabelCount>();
        sorted.stream().limit(4).forEach(e -> result.add(new LabelCount(e.getKey(), e.getValue())));
        long others = sorted.stream().skip(4).mapToLong(java.util.Map.Entry::getValue).sum();
        if (others > 0) result.add(new LabelCount("Autres", others));
        return result;
    }

    /** États financiers created in each of the last 6 months (months without any count as 0), oldest first. */
    private List<LabelCount> etatsParMois(LocalDate today) {
        var first = today.withDayOfMonth(1).minusMonths(5);
        var parMois = new java.util.LinkedHashMap<String, Long>();
        for (int i = 0; i < 6; i++) parMois.put(first.plusMonths(i).toString().substring(0, 7), 0L);
        etatFinancierRepository.findByCreateAtGreaterThanEqual(java.sql.Date.valueOf(first)).forEach(e -> {
            if (e.getCreateAt() != null) parMois.computeIfPresent(e.getCreateAt().toString().substring(0, 7), (k, v) -> v + 1);
        });
        return parMois.entrySet().stream().map(e -> new LabelCount(e.getKey(), e.getValue())).toList();
    }

    @Override
    public List<SocieteOverviewDto> societesOverview() {
        return societeRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "id")).stream()
                .map(societe -> {
                    Abonnement abonnement = abonnementService.getActiveEntity(societe.getId());
                    String planNom = abonnement == null ? null
                            : planRepository.findById(abonnement.getPlanId()).map(plan -> plan.getNom()).orElse(null);
                    return new SocieteOverviewDto(societe.getId(), societe.getNom(), societe.getCode(),
                            !Boolean.FALSE.equals(societe.getActive()), planNom,
                            abonnement == null ? null : abonnement.getDateFin(),
                            abonnementService.isCurrent(societe.getId()),
                            entrepriseRepository.countBySociete_Id(societe.getId()),
                            userSocieteRepository.countBySocieteId(societe.getId()));
                })
                .toList();
    }

    @Override
    public List<AdminUserDto> usersOverview() {
        var societeByUser = new java.util.HashMap<String, Long>();
        userSocieteRepository.findAll().forEach(member -> societeByUser.put(member.getKeycloakUserId(), member.getSocieteId()));
        var nomBySociete = new java.util.HashMap<Long, String>();
        societeRepository.findAll().forEach(societe -> nomBySociete.put(societe.getId(), societe.getNom()));

        return keycloakUserService.listUsers().stream()
                .map(user -> {
                    Long societeId = societeByUser.get(user.id());
                    return new AdminUserDto(user.id(), user.username(), user.email(), user.firstName(), user.lastName(),
                            user.enabled(), user.roles(), societeId, societeId == null ? null : nomBySociete.get(societeId));
                })
                .toList();
    }

    private MeSocieteDto.AbonnementInfo abonnementInfo(Long societeId) {
        Abonnement abonnement = abonnementService.getActiveEntity(societeId);
        if (abonnement == null) return null;
        String planNom = planRepository.findById(abonnement.getPlanId()).map(plan -> plan.getNom()).orElse(null);
        return new MeSocieteDto.AbonnementInfo(planNom, abonnement.getDateDebut(), abonnement.getDateFin(),
                abonnement.isHybride(), abonnementService.isCurrent(societeId));
    }
}
