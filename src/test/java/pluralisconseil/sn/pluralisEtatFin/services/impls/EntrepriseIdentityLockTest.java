package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseLockedException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** On a 1-entreprise plan without hybride a société can neither delete nor rename its entreprise. */
@ExtendWith(MockitoExtension.class)
class EntrepriseIdentityLockTest {

    private static final long SOCIETE = 1L;
    private static final long ENTREPRISE = 10L;

    @Mock EntrepriseRepository repository;
    @Mock AbonnementService abonnementService;
    @Mock AbonnementPlanRepository planRepository;

    private EntrepriseServiceImpl service() {
        return new EntrepriseServiceImpl(repository, new EntrepriseMapperImpl(), abonnementService, planRepository,
                org.mockito.Mockito.mock(DirigeantEntrepriseService.class), org.mockito.Mockito.mock(BanqueEntrepriseService.class),
                org.mockito.Mockito.mock(ActionaireEntrepriseService.class), org.mockito.Mockito.mock(ActiviteEntrepriseService.class),
                org.mockito.Mockito.mock(GerantEntrepriseService.class));
    }

    private void entrepriseExists() {
        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise("Acme");
        substitute.setCode_importeur("C1");
        when(repository.findById(ENTREPRISE)).thenReturn(Optional.of(Entreprise.builder()
                .id(ENTREPRISE).societe(Societe.builder().id(SOCIETE).build()).substitute(substitute).build()));
    }

    private void plan(int quota, boolean hybride) {
        when(abonnementService.getActiveEntity(SOCIETE)).thenReturn(Abonnement.builder()
                .societeId(SOCIETE).planId(7L).hybride(hybride).active(true).build());
        when(planRepository.findById(7L)).thenReturn(Optional.of(AbonnementPlan.builder().entrepriseQuota(quota).build()));
    }

    private static EntrepriseDto dto(String name, String code) {
        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise(name);
        substitute.setCode_importeur(code);
        var dto = new EntrepriseDto();
        dto.setId(ENTREPRISE);
        dto.setSubstitute(substitute);
        return dto;
    }

    @Test
    void soloNonHybride_cannotDelete() {
        entrepriseExists();
        plan(1, false);
        assertThatThrownBy(() -> service().delete(ENTREPRISE)).isInstanceOf(EntrepriseLockedException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void withoutActiveAbonnement_cannotDelete() {
        entrepriseExists();
        when(abonnementService.getActiveEntity(SOCIETE)).thenReturn(null);
        assertThatThrownBy(() -> service().delete(ENTREPRISE)).isInstanceOf(EntrepriseLockedException.class);
    }

    @Test
    void soloHybride_canDelete() {
        entrepriseExists();
        plan(1, true);
        assertThatCode(() -> service().delete(ENTREPRISE)).doesNotThrowAnyException();
        verify(repository).deleteById(ENTREPRISE);
    }

    @Test
    void multiEntreprisePlan_canDelete() {
        entrepriseExists();
        plan(5, false);
        assertThatCode(() -> service().delete(ENTREPRISE)).doesNotThrowAnyException();
    }

    @Test
    void soloNonHybride_cannotRenameOrChangeCode() {
        entrepriseExists();
        plan(1, false);
        assertThatThrownBy(() -> service().update(dto("Autre", "C1"))).isInstanceOf(EntrepriseLockedException.class);
        assertThatThrownBy(() -> service().update(dto("Acme", "C2"))).isInstanceOf(EntrepriseLockedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void identityUnchanged_canStillEditOtherFields_withoutEvenLookingAtThePlan() {
        entrepriseExists();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var edited = dto("ACME", "C1"); // same name apart from case
        edited.getSubstitute().setSigle_usuel("A");
        assertThatCode(() -> service().update(edited)).doesNotThrowAnyException();
        verify(repository).save(any());
    }
}
