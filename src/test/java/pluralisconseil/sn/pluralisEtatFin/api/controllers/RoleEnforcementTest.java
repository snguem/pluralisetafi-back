package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.*;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.helpers.*;
import pluralisconseil.sn.pluralisEtatFin.services.impls.SocieteAccessGuard;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INVITE is read-only, entreprise creation/deletion is for ADMIN_COMPTE, and an état financier can only hang off the caller's own entreprise. */
@ExtendWith(MockitoExtension.class)
class RoleEnforcementTest {

    private static final long MY_SOCIETE = 1L;
    private static final long OTHER_SOCIETE = 2L;

    @Mock UserSocieteService userSocieteService;
    @Mock EntrepriseService entrepriseService;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock EtatFinancierService etatFinancierService;
    @Mock DirigeantEntrepriseService dirigeantService;
    @Mock Authentication authentication;

    private void callerWithRole(String role) {
        var caller = new UserSocieteDto();
        caller.setSocieteId(MY_SOCIETE);
        caller.setRole(role);
        when(userSocieteService.resolveCurrent(authentication)).thenReturn(caller);
    }

    private EntrepriseRestController entrepriseController() {
        return new EntrepriseRestController(entrepriseService, mock(EntrepriseMapper.class), userSocieteService);
    }

    private EtatFinancierRestController etatFinancierController() {
        return new EtatFinancierRestController(etatFinancierService, mock(ModelExcelService.class), mock(HelperService.class),
                mock(ExcelTemplateService.class), mock(ExcelCleanupService.class), mock(ExcelToPdfConverter.class),
                mock(EtatFinancierImportService.class), userSocieteService, entrepriseService);
    }

    private static EntrepriseDto entrepriseDto() {
        var substitute = new pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute();
        substitute.setNameEntreprise("Acme");
        var dto = new EntrepriseDto();
        dto.setSubstitute(substitute);
        return dto;
    }

    @Test
    void collaborateurCannotCreateAnEntreprise() {
        callerWithRole("COLLABORATEUR");
        assertThat(entrepriseController().createEntreprise(authentication, entrepriseDto()).getStatus())
                .isEqualTo(Response.Status.UNAUTHORIZED);
        verify(entrepriseService, never()).create(any(), any());
    }

    @Test
    void collaborateurCannotDeleteAnEntreprise() {
        callerWithRole("COLLABORATEUR");
        assertThatThrownBy(() -> entrepriseController().deleteEntreprise(authentication, 10L))
                .isInstanceOf(UnauthorizeException.class);
        verify(entrepriseService, never()).delete(any());
    }

    @Test
    void inviteCannotUpdateAnEntreprise() {
        callerWithRole("INVITE");
        assertThat(entrepriseController().updateEntreprise(authentication, 10L, entrepriseDto()).getStatus())
                .isEqualTo(Response.Status.UNAUTHORIZED);
        verify(entrepriseService, never()).update(any());
    }

    @Test
    void inviteCannotWriteSubRecordsButCollaborateurCan() {
        var controller = new DirigeantEntrepriseRestController(dirigeantService);
        var guard = new SocieteAccessGuard(userSocieteService, entrepriseRepository);
        ReflectionTestUtils.setField(controller, "guard", guard);
        var dto = new DirigeantEntrepriseDto();
        dto.setEntrepriseId(10L);
        when(entrepriseRepository.findById(10L)).thenReturn(Optional.of(
                Entreprise.builder().id(10L).societe(Societe.builder().id(MY_SOCIETE).build()).build()));

        callerWithRole("INVITE");
        assertThat(controller.create(authentication, dto).getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(dirigeantService, never()).create(any());

        callerWithRole("COLLABORATEUR");
        assertThat(controller.create(authentication, dto).getStatus()).isEqualTo(Response.Status.OK);
    }

    @Test
    void inviteCannotChangeAnEtatFinancier() {
        callerWithRole("INVITE");
        var entity = EtatFinancier.builder().entrepriseRef(
                Entreprise.builder().societe(Societe.builder().id(MY_SOCIETE).build()).build()).build();
        when(etatFinancierService.getEntity(5L)).thenReturn(entity);

        assertThat(etatFinancierController().updateEtatFinancier(authentication, 5L, new EtatFinancierDto()).getStatus())
                .isEqualTo(Response.Status.UNAUTHORIZED);
        assertThatThrownBy(() -> etatFinancierController().deleteEtatFinancier(authentication, 5L))
                .isInstanceOf(UnauthorizeException.class);
        verify(etatFinancierService, never()).update(any());
        verify(etatFinancierService, never()).delete(any());
    }

    @Test
    void etatFinancierCannotBeCreatedOnAnotherSocietesEntreprise() {
        callerWithRole("ADMIN_COMPTE");
        when(entrepriseService.getEntity(20L)).thenReturn(
                Entreprise.builder().id(20L).societe(Societe.builder().id(OTHER_SOCIETE).build()).build());
        var form = new EtatFinancierFormDto();
        var substitute = new EntrepriseSubstituteDto();
        substitute.setId_(20L);
        form.setEntreprise(substitute);
        form.setEtat_financier(new EtatFinancierDto());

        assertThat(etatFinancierController().createEtatFinancier(authentication, form).getStatus())
                .isEqualTo(Response.Status.UNAUTHORIZED);
        verify(etatFinancierService, never()).create(any());
    }

    @Test
    void etatFinancierWithoutEntrepriseIdIsRejected() {
        callerWithRole("ADMIN_COMPTE");
        var form = new EtatFinancierFormDto();
        form.setEntreprise(new EntrepriseSubstituteDto());
        form.setEtat_financier(new EtatFinancierDto());

        assertThat(etatFinancierController().createEtatFinancier(authentication, form).getStatus())
                .isEqualTo(Response.Status.UNAUTHORIZED);
    }
}
