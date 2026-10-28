package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelCleanupService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelTemplateService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelToPdfConverter;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierImportService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link EtatFinancierRestController} after Task 7's refactor: the
 * balance-import endpoints now delegate to {@link EtatFinancierImportService} and the
 * controller's job is reduced to calling it and mapping the outcome to a {@link
 * pluralisconseil.sn.pluralisEtatFin.api.models.Response}.
 */
@WebMvcTest(EtatFinancierRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class EtatFinancierRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtatFinancierService service;
    @MockitoBean
    private ModelExcelService modelExcelService;
    @MockitoBean
    private HelperService helperService;
    @MockitoBean
    private ExcelTemplateService excelTemplateService;
    @MockitoBean
    private ExcelCleanupService excelCleanupService;
    @MockitoBean
    private ExcelToPdfConverter excelToPdfConverter;
    @MockitoBean
    private EtatFinancierImportService importService;
    @MockitoBean
    private UserSocieteService userSocieteService;
    @MockitoBean
    private EntrepriseService entrepriseService;

    /**
     * {@code requireOwnership} (added for société-scoping) now runs at the top of every
     * id-based endpoint this class exercises. Stub it to always resolve a caller whose société
     * matches the entity's {@code entrepriseRef.societe}, so the ownership check passes and
     * these pre-existing tests keep exercising only the behavior they were written for.
     */
    private static final Long CALLER_SOCIETE_ID = 42L;

    @BeforeEach
    void stubOwnershipCheck() {
        UserSocieteDto caller = new UserSocieteDto();
        caller.setSocieteId(CALLER_SOCIETE_ID);
        caller.setRole("ADMIN_COMPTE");
        when(userSocieteService.resolveCurrent(any())).thenReturn(caller);

        Entreprise entreprise = Entreprise.builder()
                .societe(Societe.builder().id(CALLER_SOCIETE_ID).build())
                .build();
        EtatFinancier entity = EtatFinancier.builder().entrepriseRef(entreprise).build();
        when(service.getEntity(anyLong())).thenReturn(entity);
    }

    private static MockMultipartFile balanceNPart() {
        return new MockMultipartFile("balance_n", "balance_n.xlsx", "application/vnd.ms-excel", "n".getBytes());
    }

    private static MockMultipartFile balanceN1Part() {
        return new MockMultipartFile("balance_n_1", "balance_n_1.xlsx", "application/vnd.ms-excel", "n1".getBytes());
    }

    // ------------------------------------------------------------- setBalances

    @Test
    void setBalances_success_returnsOkPayload() throws Exception {
        long id = 1L;
        EtatFinancierDto updated = new EtatFinancierDto();
        updated.setId(id);
        when(importService.importBalances(anyLong(), any(), any())).thenReturn(updated);

        mockMvc.perform(multipart(HttpMethod.PUT, "/etats-fin/{id}/annees-balances", id)
                        .file(balanceNPart())
                        .file(balanceN1Part()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Balances fusionne"))
                .andExpect(jsonPath("$.payload.id").value(1));
    }

    @Test
    void setBalances_fileNotFound_returnsNotFoundStatus() throws Exception {
        long id = 2L;
        when(importService.importBalances(anyLong(), any(), any()))
                .thenThrow(new FileNotFoundException("fichier introuvable"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/etats-fin/{id}/annees-balances", id)
                        .file(balanceNPart())
                        .file(balanceN1Part()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("fichier introuvable"));
    }

    @Test
    void setBalances_knownMergeFailureMessage_returnsExceptionStatus() throws Exception {
        long id = 3L;
        when(importService.importBalances(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("Impossible de fusionner les balances"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/etats-fin/{id}/annees-balances", id)
                        .file(balanceNPart())
                        .file(balanceN1Part()))
                .andExpect(jsonPath("$.status").value("EXCEPTION"))
                .andExpect(jsonPath("$.message").value("Impossible de fusionner les balances"));
    }

    @Test
    void setBalances_knownConfigFailureTypoedMessage_returnsExceptionStatus() throws Exception {
        // Distinct from setBalances_knownMergeFailureMessage_returnsExceptionStatus above: this
        // is finalizeFusedImport's own null-configExcelFile message, which carries the preserved
        // pre-existing typo "produitehgd".
        long id = 7L;
        when(importService.importBalances(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("Une erreur s'est produitehgd lors de la configuration des fichiers"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/etats-fin/{id}/annees-balances", id)
                        .file(balanceNPart())
                        .file(balanceN1Part()))
                .andExpect(jsonPath("$.status").value("EXCEPTION"))
                .andExpect(jsonPath("$.message").value("Une erreur s'est produitehgd lors de la configuration des fichiers"));
    }

    @Test
    void setBalances_unknownException_returnsBadRequestStatus() throws Exception {
        long id = 4L;
        when(importService.importBalances(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/etats-fin/{id}/annees-balances", id)
                        .file(balanceNPart())
                        .file(balanceN1Part()))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("boom"));
    }

    @Test
    void setBalances_exceptionWithNullMessage_returnsBadRequestNotUnhandled500() throws Exception {
        long id = 6L;
        when(importService.importBalances(anyLong(), any(), any()))
                .thenThrow(new RuntimeException((String) null));

        mockMvc.perform(multipart(HttpMethod.PUT, "/etats-fin/{id}/annees-balances", id)
                        .file(balanceNPart())
                        .file(balanceN1Part()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    // ------------------------------------------------------------- getPageNames (unaffected)

    @Test
    void getPageNames_stillWorks() throws Exception {
        long id = 5L;
        EtatFinancierDto dto = new EtatFinancierDto();
        dto.setId(id);
        dto.setExcelPath("some.xlsx");
        when(service.get(id)).thenReturn(dto);
        when(excelTemplateService.getExcelPageNames("some.xlsx")).thenReturn(List.of("Page1", "Page2"));

        mockMvc.perform(get("/etats-fin/{id}/page-names", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.payload[0]").value("Page1"));
    }

    // ------------------------------------------------------------- getAllEtatFinanciers (/all)

    @Test
    void getAllEtatFinanciers_scopesQueryToCallersSociete_notUnscopedGetAll() throws Exception {
        when(service.getAllBySociete(eq(CALLER_SOCIETE_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new EtatFinancierDto())));

        mockMvc.perform(get("/etats-fin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));

        verify(service, never()).getAll(any(), any());
    }

    // ------------------------------------------------------- getAllEtatFinanciersEntreprise (/all-entreprise)

    @Test
    void getAllEtatFinanciersEntreprise_withEntrepriseOfCallersSociete_returnsOk() throws Exception {
        long entrepriseId = 10L;
        Entreprise entreprise = Entreprise.builder()
                .societe(Societe.builder().id(CALLER_SOCIETE_ID).build())
                .build();
        when(entrepriseService.getEntity(entrepriseId)).thenReturn(entreprise);
        when(service.getAllByEntreprise(eq(entrepriseId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new EtatFinancierDto())));

        mockMvc.perform(get("/etats-fin/all-entreprise").param("entreprise_id", String.valueOf(entrepriseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void getAllEtatFinanciersEntreprise_withEntrepriseOfAnotherSociete_returnsUnauthorized() throws Exception {
        long entrepriseId = 11L;
        Entreprise entrepriseOfAnotherSociete = Entreprise.builder()
                .societe(Societe.builder().id(CALLER_SOCIETE_ID + 1).build())
                .build();
        when(entrepriseService.getEntity(entrepriseId)).thenReturn(entrepriseOfAnotherSociete);

        mockMvc.perform(get("/etats-fin/all-entreprise").param("entreprise_id", String.valueOf(entrepriseId)))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(service, never()).getAllByEntreprise(anyLong(), any(), any());
    }
}
