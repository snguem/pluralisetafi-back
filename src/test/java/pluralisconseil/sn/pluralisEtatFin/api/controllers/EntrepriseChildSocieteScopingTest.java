package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.services.impls.SocieteAccessGuard;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DirigeantEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A société must only reach entreprise sub-records (dirigeants, ...) through its own entreprises. */
@ExtendWith(MockitoExtension.class)
class EntrepriseChildSocieteScopingTest {

    private static final long MY_SOCIETE = 1L;
    private static final long OTHER_SOCIETE = 2L;
    private static final long MY_ENTREPRISE = 10L;
    private static final long OTHER_ENTREPRISE = 20L;

    @Mock UserSocieteService userSocieteService;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock DirigeantEntrepriseService service;
    @Mock Authentication authentication;

    private DirigeantEntrepriseRestController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new DirigeantEntrepriseRestController(service);
        ReflectionTestUtils.setField(controller, "guard", new SocieteAccessGuard(userSocieteService, entrepriseRepository));

        var caller = new UserSocieteDto();
        caller.setSocieteId(MY_SOCIETE);
        caller.setRole("COLLABORATEUR");
        when(userSocieteService.resolveCurrent(authentication)).thenReturn(caller);
        lenient(MY_ENTREPRISE, MY_SOCIETE);
        lenient(OTHER_ENTREPRISE, OTHER_SOCIETE);
    }

    private void lenient(long entrepriseId, long societeId) {
        var e = Entreprise.builder().id(entrepriseId).societe(Societe.builder().id(societeId).build()).build();
        org.mockito.Mockito.lenient().when(entrepriseRepository.findById(entrepriseId)).thenReturn(Optional.of(e));
    }

    private static DirigeantEntrepriseDto dto(Long entrepriseId) {
        var dto = new DirigeantEntrepriseDto();
        dto.setEntrepriseId(entrepriseId);
        return dto;
    }

    @Test
    void createOnOwnEntrepriseSucceeds() {
        when(service.create(any())).thenReturn(dto(MY_ENTREPRISE));
        assertThat(controller.create(authentication, dto(MY_ENTREPRISE)).getStatus()).isEqualTo(Response.Status.OK);
    }

    @Test
    void createOnOtherSocietesEntrepriseIsRejected() {
        var res = controller.create(authentication, dto(OTHER_ENTREPRISE));
        assertThat(res.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(service, never()).create(any());
    }

    @Test
    void createWithoutEntrepriseIsRejected() {
        var res = controller.create(authentication, dto(null));
        assertThat(res.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(service, never()).create(any());
    }

    @Test
    void getOtherSocietesRecordIsRejected() {
        when(service.getEntrepriseId(5L)).thenReturn(OTHER_ENTREPRISE);
        assertThat(controller.get(authentication, 5L).getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(service, never()).get(any());
    }

    @Test
    void updateCannotMoveRecordToAnotherSociete() {
        when(service.getEntrepriseId(5L)).thenReturn(MY_ENTREPRISE);
        when(service.update(any())).thenAnswer(i -> i.getArgument(0));
        var res = controller.update(authentication, 5L, dto(OTHER_ENTREPRISE));
        assertThat(res.getStatus()).isEqualTo(Response.Status.OK);
        assertThat(((DirigeantEntrepriseDto) res.getPayload()).getEntrepriseId()).isEqualTo(MY_ENTREPRISE);
    }

    @Test
    void updateOtherSocietesRecordIsRejected() {
        when(service.getEntrepriseId(5L)).thenReturn(OTHER_ENTREPRISE);
        assertThat(controller.update(authentication, 5L, dto(MY_ENTREPRISE)).getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(service, never()).update(any());
    }

    @Test
    void deleteOtherSocietesRecordIsRejected() {
        when(service.getEntrepriseId(5L)).thenReturn(OTHER_ENTREPRISE);
        assertThatThrownBy(() -> controller.delete(authentication, 5L)).isInstanceOf(UnauthorizeException.class);
        verify(service, never()).delete(any());
    }

    @Test
    void listByOtherSocietesEntrepriseIsRejected() {
        var res = controller.getAllEntreprise(authentication, Map.of(), PageRequest.of(0, 10), OTHER_ENTREPRISE);
        assertThat(res.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
    }

    @Test
    void listAllIsRestrictedToCallersSociete() {
        var pageable = PageRequest.of(0, 10);
        when(service.getAllBySociete(MY_SOCIETE, pageable)).thenReturn(new PageImpl<>(List.of(dto(MY_ENTREPRISE))));
        var res = controller.getAll(authentication, Map.of(), pageable);
        assertThat(res.getStatus()).isEqualTo(Response.Status.OK);
        verify(service, never()).getAll(any(), any());
    }
}
