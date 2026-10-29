package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.*;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.*;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final long SOCIETE = 1L;

    @Mock AbonnementPlanRepository planRepository;
    @Mock AbonnementRepository abonnementRepository;
    @Mock AbonnementService abonnementService;
    @Mock SocieteRepository societeRepository;
    @Mock UserSocieteRepository userSocieteRepository;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock EtatFinancierRepository etatFinancierRepository;
    @Mock pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService keycloakUserService;

    private DashboardServiceImpl service() {
        return new DashboardServiceImpl(planRepository, abonnementRepository, abonnementService, societeRepository,
                userSocieteRepository, entrepriseRepository, etatFinancierRepository, keycloakUserService);
    }

    private static UserSocieteDto caller(boolean readOnly) {
        var caller = new UserSocieteDto();
        caller.setSocieteId(SOCIETE);
        caller.setRole("COLLABORATEUR");
        caller.setReadOnly(readOnly);
        return caller;
    }

    private void abonnement(boolean current) {
        var today = LocalDate.now();
        when(abonnementService.getActiveEntity(SOCIETE)).thenReturn(Abonnement.builder().societeId(SOCIETE).planId(7L)
                .dateDebut(today.minusDays(5)).dateFin(today.plusDays(25)).hybride(true).active(true).build());
        when(planRepository.findById(7L)).thenReturn(Optional.of(
                AbonnementPlan.builder().nom("Cabinet").entrepriseQuota(5).build()));
        when(abonnementService.isCurrent(SOCIETE)).thenReturn(current);
    }

    @Test
    void publicPlans_listsTheActivePlansOnly_withTheirPrices() {
        when(planRepository.findByActiveTrueOrderByPrixParMoisAsc()).thenReturn(List.of(AbonnementPlan.builder()
                .id(1L).nom("Solo").entrepriseQuota(1).prixParMois(new BigDecimal("15000")).active(true).build()));

        var plans = service().publicPlans();

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).nom()).isEqualTo("Solo");
        assertThat(plans.get(0).prixParMois()).isEqualByComparingTo("15000");
    }

    @Test
    void meSociete_carriesTheRoleTheReadOnlyFlagAndTheAbonnement() {
        when(societeRepository.findById(SOCIETE)).thenReturn(Optional.of(Societe.builder().id(SOCIETE).nom("Cabinet Fatou").build()));
        abonnement(false);

        var me = service().meSociete(caller(true));

        assertThat(me.societeNom()).isEqualTo("Cabinet Fatou");
        assertThat(me.role()).isEqualTo("COLLABORATEUR");
        assertThat(me.readOnly()).isTrue();
        assertThat(me.abonnement().planNom()).isEqualTo("Cabinet");
        assertThat(me.abonnement().current()).isFalse();
    }

    @Test
    void societeStats_countsEverythingOfTheSocieteOnly() {
        abonnement(true);
        when(entrepriseRepository.countBySociete_Id(SOCIETE)).thenReturn(2L);
        when(etatFinancierRepository.countByEntrepriseRef_Societe_Id(SOCIETE)).thenReturn(9L);
        when(userSocieteRepository.countBySocieteId(SOCIETE)).thenReturn(4L);
        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise("Acme");
        when(etatFinancierRepository.findTop5ByEntrepriseRef_Societe_IdOrderByIdDesc(SOCIETE)).thenReturn(List.of(
                EtatFinancier.builder().id(3L).name("Etat Acme 2025").annee_n(2025)
                        .entrepriseRef(Entreprise.builder().substitute(substitute).build()).build()));

        var acme = Entreprise.builder().substitute(substitute).build();
        var baobabSubstitute = new EntrepriseDatasSubstitute();
        baobabSubstitute.setNameEntreprise("Baobab");
        var baobab = Entreprise.builder().substitute(baobabSubstitute).build();
        when(etatFinancierRepository.findByEntrepriseRef_Societe_Id(SOCIETE)).thenReturn(List.of(
                EtatFinancier.builder().annee_n(2024).entrepriseRef(acme).build(),
                EtatFinancier.builder().annee_n(2025).entrepriseRef(acme).build(),
                EtatFinancier.builder().annee_n(2025).entrepriseRef(baobab).build()));

        var stats = service().societeStats(caller(false));

        assertThat(stats.parAnnee()).extracting("label", "count").containsExactly(
                org.assertj.core.groups.Tuple.tuple("2024", 1L), org.assertj.core.groups.Tuple.tuple("2025", 2L));
        assertThat(stats.parEntreprise()).extracting("label", "count").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Acme", 2L), org.assertj.core.groups.Tuple.tuple("Baobab", 1L));
        assertThat(stats.entreprises()).isEqualTo(2);
        assertThat(stats.entrepriseQuota()).isEqualTo(5);
        assertThat(stats.etatsFinanciers()).isEqualTo(9);
        assertThat(stats.membres()).isEqualTo(4);
        assertThat(stats.derniersEtatsFinanciers()).extracting("entrepriseNom").containsExactly("Acme");
    }

    @Test
    void societeStats_withoutAbonnement_hasAQuotaOfZeroAndNoAbonnement() {
        when(abonnementService.getActiveEntity(SOCIETE)).thenReturn(null);
        when(etatFinancierRepository.findTop5ByEntrepriseRef_Societe_IdOrderByIdDesc(SOCIETE)).thenReturn(List.of());

        var stats = service().societeStats(caller(true));

        assertThat(stats.entrepriseQuota()).isZero();
        assertThat(stats.abonnement()).isNull();
    }

    @Test
    void platformStats_countsAndBuildsTheChartsOfThePlatform() {
        var today = LocalDate.now();
        when(societeRepository.count()).thenReturn(10L);
        when(societeRepository.countByActiveTrue()).thenReturn(8L);
        when(abonnementRepository.countByActiveTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(6L);
        when(abonnementRepository.countByActiveTrueAndDateFinBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(2L);
        when(userSocieteRepository.count()).thenReturn(30L);
        when(entrepriseRepository.count()).thenReturn(12L);
        when(etatFinancierRepository.count()).thenReturn(40L);

        when(societeRepository.findAll()).thenReturn(List.of(
                Societe.builder().id(1L).nom("Cabinet").build(), Societe.builder().id(2L).nom("Autre").build()));
        when(planRepository.findAll()).thenReturn(List.of(AbonnementPlan.builder().id(7L).nom("Cabinet+").build()));
        when(abonnementRepository.findByActiveTrue()).thenReturn(List.of(
                Abonnement.builder().societeId(1L).planId(7L).active(true).dateDebut(today.minusDays(20)).dateFin(today.plusDays(10)).build(),
                Abonnement.builder().societeId(2L).planId(7L).active(true).dateDebut(today.minusDays(20)).dateFin(today.plusDays(200)).build(),
                Abonnement.builder().societeId(3L).planId(7L).active(true).dateDebut(today.minusDays(90)).dateFin(today.minusDays(1)).build()));
        when(societeRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        Societe.builder().id(2L).nom("Autre").active(false).build())));

        var stats = service().platformStats();

        assertThat(stats.societes()).isEqualTo(10);
        assertThat(stats.abonnementsEnCours()).isEqualTo(6);
        assertThat(stats.abonnementsExpirantSous30Jours()).isEqualTo(2);
        assertThat(stats.utilisateurs()).isEqualTo(30);
        // only the 2 abonnements that cover today are counted by formula; the expired one is not
        assertThat(stats.repartitionFormules()).extracting("label", "count")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Cabinet+", 2L));
        // the one ending in 10 days is announced, the one in 200 days and the expired one are not
        assertThat(stats.prochainesExpirations()).hasSize(1);
        assertThat(stats.prochainesExpirations().get(0).societeNom()).isEqualTo("Cabinet");
        assertThat(stats.prochainesExpirations().get(0).joursRestants()).isEqualTo(10);
        assertThat(stats.etatsParMois()).hasSize(6);
        assertThat(stats.dernieresSocietes().get(0).active()).isFalse();
    }

    @Test
    void societesOverview_listsEachSocieteWithItsCurrentAbonnementAndSize() {
        when(societeRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(Societe.builder().id(1L).nom("Cabinet").active(true).build(),
                        Societe.builder().id(2L).nom("Vide").active(false).build()));
        abonnement(true);
        when(abonnementService.getActiveEntity(2L)).thenReturn(null);
        when(abonnementService.isCurrent(2L)).thenReturn(false);
        when(entrepriseRepository.countBySociete_Id(1L)).thenReturn(3L);
        when(entrepriseRepository.countBySociete_Id(2L)).thenReturn(0L);
        when(userSocieteRepository.countBySocieteId(1L)).thenReturn(4L);
        when(userSocieteRepository.countBySocieteId(2L)).thenReturn(1L);

        var overview = service().societesOverview();

        assertThat(overview).hasSize(2);
        assertThat(overview.get(0).planNom()).isEqualTo("Cabinet");
        assertThat(overview.get(0).abonnementCurrent()).isTrue();
        assertThat(overview.get(0).entreprises()).isEqualTo(3);
        assertThat(overview.get(1).active()).isFalse();
        assertThat(overview.get(1).planNom()).isNull();
        assertThat(overview.get(1).abonnementCurrent()).isFalse();
    }

    @Test
    void usersOverview_addsTheSocieteOfEachAccount() {
        when(keycloakUserService.listUsers()).thenReturn(List.of(
                new pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService.KeycloakUserSummary(
                        "u1", "fatou", "f@x.sn", "Fatou", "Ndiaye", true, List.of("COLLABORATEUR")),
                new pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService.KeycloakUserSummary(
                        "u2", "awa", "a@x.sn", "Awa", "Sy", true, List.of("ADMIN_PLATEFORME"))));
        when(userSocieteRepository.findAll()).thenReturn(List.of(
                UserSociete.builder().keycloakUserId("u1").societeId(1L).role("COLLABORATEUR").build()));
        when(societeRepository.findAll()).thenReturn(List.of(Societe.builder().id(1L).nom("Cabinet").build()));

        var users = service().usersOverview();

        assertThat(users.get(0).societeNom()).isEqualTo("Cabinet");
        assertThat(users.get(1).societeId()).isNull();
        assertThat(users.get(1).roles()).containsExactly("ADMIN_PLATEFORME");
    }
}
