package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseSubstituteDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.helpers.BalanceMergeService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelCleanupService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelFormulaCalculator;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterization/unit tests for {@link EtatFinancierImportServiceImpl}, which replicates the
 * import logic currently inline in {@code EtatFinancierRestController#setBalances},
 * {@code #setBalancesWithEtatFinN1}, {@code #setBalanceN} and the private helper
 * {@code #getObjectResponse}.
 */
@ExtendWith(MockitoExtension.class)
class EtatFinancierImportServiceImplTest {

    @Mock
    EtatFinancierService etatFinancierService;
    @Mock
    ModelExcelService modelExcelService;
    @Mock
    HelperService helperService;
    @Mock
    BalanceMergeService balanceMergeService;
    @Mock
    ExcelCleanupService excelCleanupService;
    @Mock
    ExcelFormulaCalculator excelFormulaCalculator;

    @InjectMocks
    EtatFinancierImportServiceImpl service;

    private static EtatFinancierDto newDto(Long id, Long modelExcelId, String excelPath) {
        EtatFinancierDto dto = new EtatFinancierDto();
        dto.setId(id);
        dto.setModelExcel_id(modelExcelId);
        dto.setExcelPath(excelPath);
        EntrepriseSubstituteDto entreprise = new EntrepriseSubstituteDto();
        entreprise.setNameEntreprise("Acme");
        dto.setEntreprise(entreprise);
        return dto;
    }

    // ---------------------------------------------------------------- importBalances

    @Test
    void importBalances_mergesCleansCalculatesAndUpdates() throws Exception {
        long id = 1L;
        EtatFinancierDto dto = newDto(id, 5L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);
        MultipartFile balanceN1 = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(5L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn("temp/balance_n.xlsx");
        when(helperService.saveTempFile(balanceN1)).thenReturn("temp/balance_n_1.xlsx");

        EtatFinancierDto fused = newDto(id, 5L, "fused.xlsx");
        when(balanceMergeService.fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx", "temp/balance_n_1.xlsx"))
                .thenReturn(fused);

        EtatFinancierDto configured = newDto(id, 5L, "out.xlsx");
        when(balanceMergeService.configExcelFile(fused, dto.getEntreprise())).thenReturn(configured);

        EtatFinancierDto updated = newDto(id, 5L, "out.xlsx");
        when(etatFinancierService.update(configured)).thenReturn(updated);

        EtatFinancierDto result = service.importBalances(id, balanceN, balanceN1);

        assertThat(result).isSameAs(updated);

        InOrder inOrder = inOrder(balanceMergeService, excelCleanupService, excelFormulaCalculator, etatFinancierService);
        inOrder.verify(balanceMergeService).fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx", "temp/balance_n_1.xlsx");
        inOrder.verify(balanceMergeService).configExcelFile(fused, dto.getEntreprise());
        inOrder.verify(excelCleanupService).nettoyerEtRecupererExcel("out.xlsx");
        inOrder.verify(excelFormulaCalculator).calculateFormule("out.xlsx");
        inOrder.verify(etatFinancierService).update(configured);

        verify(helperService).removesTemp(List.of("temp/balance_n.xlsx", "temp/balance_n_1.xlsx"));
        verify(etatFinancierService, never()).delete(any());
    }

    @Test
    void importBalances_onFailure_deletesEtatFinancierAndRethrows() throws Exception {
        long id = 1L;
        EtatFinancierDto dto = newDto(id, 5L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);
        MultipartFile balanceN1 = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(5L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn("temp/balance_n.xlsx");
        when(helperService.saveTempFile(balanceN1)).thenReturn("temp/balance_n_1.xlsx");
        when(balanceMergeService.fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx", "temp/balance_n_1.xlsx"))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.importBalances(id, balanceN, balanceN1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        verify(etatFinancierService).delete(id);
    }

    @Test
    void importBalances_whenConfigExcelFileReturnsNull_throwsTypoedMessageAndDeletesEtatFinancier() throws Exception {
        // Exercises finalizeFusedImport's own null-configExcelFile branch (reached via
        // importBalances), which is distinct from importBalanceN's separate inline branch:
        // the message here carries the preserved pre-existing typo "produitehgd".
        long id = 1L;
        EtatFinancierDto dto = newDto(id, 5L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);
        MultipartFile balanceN1 = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(5L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn("temp/balance_n.xlsx");
        when(helperService.saveTempFile(balanceN1)).thenReturn("temp/balance_n_1.xlsx");

        EtatFinancierDto fused = newDto(id, 5L, "fused.xlsx");
        when(balanceMergeService.fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx", "temp/balance_n_1.xlsx"))
                .thenReturn(fused);
        when(balanceMergeService.configExcelFile(fused, dto.getEntreprise())).thenReturn(null);

        assertThatThrownBy(() -> service.importBalances(id, balanceN, balanceN1))
                .isInstanceOf(Exception.class)
                .hasMessage("Une erreur s'est produitehgd lors de la configuration des fichiers");

        verify(etatFinancierService).delete(id);
        verify(etatFinancierService, never()).update(any());
    }

    @Test
    void importBalances_whenTempFilesMissing_throwsCleansTempButDoesNotDeleteEtatFinancier() throws Exception {
        // Replicates the controller's current setBalances: the missing-temp-file branch does a
        // plain `return Response.exception()...` INSIDE the try block, so it never reaches the
        // catch blocks and service.delete(id) is NOT called for this specific failure (unlike
        // every other failure in this method).
        long id = 1L;
        EtatFinancierDto dto = newDto(id, 5L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);
        MultipartFile balanceN1 = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(5L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn(null);
        when(helperService.saveTempFile(balanceN1)).thenReturn("temp/balance_n_1.xlsx");

        assertThatThrownBy(() -> service.importBalances(id, balanceN, balanceN1))
                .isInstanceOf(Exception.class)
                .hasMessage("Impossible de fusionner les balances");

        verify(balanceMergeService, never()).fuseBalanceToExcel(any(), any(), any(), any());
        verify(helperService).removesTemp(anyList());
        verify(etatFinancierService, never()).delete(any());
    }

    // ---------------------------------------------------------------- importBalanceN

    @Test
    void importBalanceN_mergesCleansCalculatesAndUpdates() throws Exception {
        long id = 2L;
        EtatFinancierDto dto = newDto(id, 7L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(7L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn("temp/balance_n.xlsx");

        EtatFinancierDto fused = newDto(id, 7L, "fused.xlsx");
        when(balanceMergeService.fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx")).thenReturn(fused);

        EtatFinancierDto configured = newDto(id, 7L, "out.xlsx");
        when(balanceMergeService.configExcelFile(fused, dto.getEntreprise())).thenReturn(configured);

        EtatFinancierDto updated = newDto(id, 7L, "out.xlsx");
        when(etatFinancierService.update(configured)).thenReturn(updated);

        EtatFinancierDto result = service.importBalanceN(id, balanceN);

        assertThat(result).isSameAs(updated);
        verify(excelCleanupService).nettoyerEtRecupererExcel("out.xlsx");
        verify(excelFormulaCalculator).calculateFormule("out.xlsx");
        verify(helperService).removesTemp(List.of("temp/balance_n.xlsx"));
        verify(etatFinancierService, never()).delete(any());
    }

    @Test
    void importBalanceN_onFailure_deletesEtatFinancierAndRethrows() throws Exception {
        long id = 2L;
        EtatFinancierDto dto = newDto(id, 7L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(7L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn("temp/balance_n.xlsx");
        when(balanceMergeService.fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx"))
                .thenThrow(new RuntimeException("merge failed"));

        assertThatThrownBy(() -> service.importBalanceN(id, balanceN))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("merge failed");

        verify(etatFinancierService).delete(id);
    }

    @Test
    void importBalanceN_whenConfigExcelFileReturnsNull_throwsAndDeletesEtatFinancier() throws Exception {
        long id = 2L;
        EtatFinancierDto dto = newDto(id, 7L, "original.xlsx");
        ModelExcelDto model = new ModelExcelDto();
        model.setExcelPath("model.xlsx");

        MultipartFile balanceN = mock(MultipartFile.class);

        when(etatFinancierService.get(id)).thenReturn(dto);
        when(modelExcelService.get(7L)).thenReturn(model);
        when(helperService.saveTempFile(balanceN)).thenReturn("temp/balance_n.xlsx");

        EtatFinancierDto fused = newDto(id, 7L, "fused.xlsx");
        when(balanceMergeService.fuseBalanceToExcel(dto, "model.xlsx", "temp/balance_n.xlsx")).thenReturn(fused);
        when(balanceMergeService.configExcelFile(fused, dto.getEntreprise())).thenReturn(null);

        assertThatThrownBy(() -> service.importBalanceN(id, balanceN))
                .isInstanceOf(Exception.class)
                .hasMessage("Une erreur s'est produitehgd lors de la configuration des fichiers");

        verify(etatFinancierService).delete(id);
        verify(etatFinancierService, never()).update(any());
    }
}
