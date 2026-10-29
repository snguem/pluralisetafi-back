package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The registre de commerce identifies an entreprise across every société. */
@ExtendWith(MockitoExtension.class)
class EntrepriseRegistreUniquenessTest {

    @Mock UserSocieteService userSocieteService;
    @Mock EntrepriseService entrepriseService;
    @Mock Authentication authentication;

    private EntrepriseDto dto(String registre) {
        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise("Acme");
        substitute.setRegistre_commerce(registre);
        var dto = new EntrepriseDto();
        dto.setSubstitute(substitute);
        return dto;
    }

    private EntrepriseRestController controller() {
        var caller = new UserSocieteDto();
        caller.setSocieteId(1L);
        caller.setRole("ADMIN_COMPTE");
        when(userSocieteService.resolveCurrent(authentication)).thenReturn(caller);
        return new EntrepriseRestController(entrepriseService, mock(EntrepriseMapper.class), userSocieteService);
    }

    @Test
    void createIsRefusedWhenAnotherEntrepriseHasTheSameRegistre() {
        var controller = controller();
        var existing = new EntrepriseDto();
        existing.setId(99L);
        when(entrepriseService.getByRegistreCommerce("RC-1")).thenReturn(existing);

        assertThat(controller.createEntreprise(authentication, dto("RC-1")).getStatus())
                .isEqualTo(Response.Status.DUPLICATE_REFERENCE);
        verify(entrepriseService, never()).create(any(), any());
    }

    @Test
    void createIsAllowedWhenTheRegistreIsFree() {
        var controller = controller();
        when(entrepriseService.getByRegistreCommerce("RC-2")).thenReturn(null);

        assertThat(controller.createEntreprise(authentication, dto("RC-2")).getStatus()).isEqualTo(Response.Status.OK);
        verify(entrepriseService).create(any(), any());
    }

    private static pluralisconseil.sn.pluralisEtatFin.api.records.EntrepriseExportDto exportFile(EntrepriseDto entreprise) {
        return new pluralisconseil.sn.pluralisEtatFin.api.records.EntrepriseExportDto(
                entreprise, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
    }

    @Test
    void importIsRefusedWhenTheRegistreIsAlreadyTaken() {
        var controller = controller();
        var existing = new EntrepriseDto();
        existing.setId(99L);
        when(entrepriseService.getByRegistreCommerce("RC-1")).thenReturn(existing);

        assertThat(controller.importEntreprise(authentication, exportFile(dto("RC-1"))).getStatus())
                .isEqualTo(Response.Status.DUPLICATE_REFERENCE);
        verify(entrepriseService, never()).importExport(any(), any());
    }

    @Test
    void importCreatesTheEntrepriseInTheCallersSociete() {
        var controller = controller();
        var file = exportFile(dto("RC-3"));

        assertThat(controller.importEntreprise(authentication, file).getStatus()).isEqualTo(Response.Status.OK);
        verify(entrepriseService).importExport(file, 1L);
    }

    @Test
    void importWithoutAnEntrepriseIsABadRequest() {
        var controller = controller();

        assertThat(controller.importEntreprise(authentication, exportFile(null)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST);
    }
}
