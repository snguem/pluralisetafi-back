package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pluralisconseil.sn.pluralisEtatFin.api.controllers.EntrepriseRestController;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.InsufficientRoleException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A société whose abonnement does not cover today is read-only: it can still read and export, not write. */
@ExtendWith(MockitoExtension.class)
class ExpiredAbonnementTest {

    private static final long SOCIETE = 1L;

    @Mock AbonnementRepository abonnementRepository;
    @Mock UserSocieteRepository userSocieteRepository;
    @Mock SocieteRepository societeRepository;
    @Mock KeycloakUserService keycloakUserService;
    @Mock AbonnementService abonnementService;
    @Mock UserSocieteService userSocieteService;
    @Mock EntrepriseService entrepriseService;
    @Mock Authentication authentication;

    private void abonnement(int startOffsetDays, int endOffsetDays) {
        var today = LocalDate.now();
        when(abonnementRepository.findBySocieteIdAndActiveTrue(SOCIETE)).thenReturn(Abonnement.builder()
                .societeId(SOCIETE).active(true)
                .dateDebut(today.plusDays(startOffsetDays)).dateFin(today.plusDays(endOffsetDays)).build());
    }

    private AbonnementServiceImpl abonnements() {
        return new AbonnementServiceImpl(abonnementRepository, mock(AbonnementPlanRepository.class), societeRepository,
                new AbonnementMapperImpl());
    }

    @Test
    void isCurrent_coversTodayInclusive() {
        abonnement(-30, 0);
        assertThat(abonnements().isCurrent(SOCIETE)).isTrue();
    }

    @Test
    void isCurrent_falseOnceExpired() {
        abonnement(-60, -1);
        assertThat(abonnements().isCurrent(SOCIETE)).isFalse();
    }

    @Test
    void isCurrent_falseBeforeItStarts() {
        abonnement(1, 30);
        assertThat(abonnements().isCurrent(SOCIETE)).isFalse();
    }

    @Test
    void isCurrent_falseWithoutAbonnement() {
        when(abonnementRepository.findBySocieteIdAndActiveTrue(SOCIETE)).thenReturn(null);
        assertThat(abonnements().isCurrent(SOCIETE)).isFalse();
    }

    private UserSocieteServiceImpl userSocietes() {
        when(userSocieteRepository.findByKeycloakUserId("u1")).thenReturn(Optional.of(
                UserSociete.builder().keycloakUserId("u1").societeId(SOCIETE).role("ADMIN_COMPTE").build()));
        when(societeRepository.findById(SOCIETE)).thenReturn(Optional.of(Societe.builder().id(SOCIETE).active(true).build()));
        return new UserSocieteServiceImpl(userSocieteRepository, new UserSocieteMapperImpl(), keycloakUserService,
                societeRepository, abonnementService);
    }

    @Test
    void memberOfAnExpiredSociete_isReadOnly_andCannotAddMembers() {
        when(abonnementService.isCurrent(SOCIETE)).thenReturn(false);
        var service = userSocietes();

        assertThat(service.resolveByKeycloakUserId("u1").isReadOnly()).isTrue();
        assertThatThrownBy(() -> service.createMember("u1", "n", "n@x.sn", "N", "N", "Temp1234!", "COLLABORATEUR"))
                .isInstanceOf(InsufficientRoleException.class);
        verify(keycloakUserService, never()).createCollaborator(any(), any(), any(), any(), any(), any());
    }

    @Test
    void memberOfASocieteWithACurrentAbonnement_isNotReadOnly() {
        when(abonnementService.isCurrent(SOCIETE)).thenReturn(true);
        assertThat(userSocietes().resolveByKeycloakUserId("u1").isReadOnly()).isFalse();
    }

    @Test
    void guard_rejectsWritesOfAReadOnlySociete_evenForItsAdmin() {
        var caller = new UserSocieteDto();
        caller.setRole("ADMIN_COMPTE");
        caller.setReadOnly(true);

        assertThatThrownBy(() -> SocieteAccessGuard.requireWrite(caller)).isInstanceOf(UnauthorizeException.class);
        assertThatThrownBy(() -> SocieteAccessGuard.requireAdminCompte(caller)).isInstanceOf(UnauthorizeException.class);
    }

    @Test
    void expiredSocieteAdmin_canStillExportItsEntrepriseButNotCreateOne() {
        var caller = new UserSocieteDto();
        caller.setSocieteId(SOCIETE);
        caller.setRole("ADMIN_COMPTE");
        caller.setReadOnly(true);
        when(userSocieteService.resolveCurrent(authentication)).thenReturn(caller);
        var entreprise = Entreprise.builder().id(10L).societe(Societe.builder().id(SOCIETE).build()).build();
        when(entrepriseService.getEntity(10L)).thenReturn(entreprise);
        var controller = new EntrepriseRestController(entrepriseService, mock(EntrepriseMapper.class), userSocieteService);

        assertThat(controller.exportEntreprise(authentication, 10L).getStatus()).isEqualTo(Response.Status.OK);

        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise("Acme");
        var dto = new EntrepriseDto();
        dto.setSubstitute(substitute);
        assertThat(controller.createEntreprise(authentication, dto).getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
    }
}
