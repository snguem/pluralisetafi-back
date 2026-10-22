package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelCleanupService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelTemplateService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelToPdfConverter;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierImportService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
}
