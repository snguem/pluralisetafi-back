package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseSubstituteDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeAttributIsConfig;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.helpers.JsonService;
import pluralisconseil.sn.pluralisEtatFin.helpers.support.WorkbookTestFixtures;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActionaireEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActiviteEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.BanqueEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ConfigModelExcelService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DirigeantEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.GerantEntrepriseService;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for {@link BalanceMergeServiceImpl}. Moved verbatim (renamed class,
 * retargeted to {@code balanceMergeService}) from {@code ExcelServiceCharacterizationTest},
 * which characterized {@code ExcelService#fuseBalanceToExcel} before {@code BalanceMergeService}
 * was extracted from it. These pin down the CURRENT behavior of the moved methods (including
 * their quirks, e.g. the 200-row limit, the NullPointerException when the model lacks a "BAL N"
 * sheet, and the hardcoded output path of the 3-arg {@code complement} overload) using workbooks
 * built entirely in memory with Apache POI, rather than relying on the project's binary .xlsx
 * fixtures.
 */
class BalanceMergeServiceImplTest {

    // Only helperService.isExist(...) is exercised by addBalanceToModel/fuseBalanceToExcel, and
    // only jsonService.getAllMatrice(...) is exercised by complement; the other collaborators of
    // BalanceMergeServiceImpl are not used on these code paths, so they are left null rather
    // than mocked.
    private final BalanceMergeServiceImpl balanceMergeService =
            new BalanceMergeServiceImpl(new HelperService(), new JsonService(), null, null, null, null, null, null);

    private static EtatFinancierDto newDto() {
        EtatFinancierDto dto = new EtatFinancierDto();
        dto.setId(1L);

        EntrepriseSubstituteDto entreprise = new EntrepriseSubstituteDto();
        entreprise.setNameEntreprise("Acme");
        dto.setEntreprise(entreprise);

        dto.setAnnee_n(2025);
        return dto;
    }

    @Test
    void fuseBalanceToExcel_copiesAccountRowsIntoBalNSheet() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheet("BAL N", 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbook(List.of(
                new Object[]{"601000", "Achats", 150000.0},
                new Object[]{"701000", "Ventes", -300000.0}
        ));
        EtatFinancierDto dto = newDto();

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(dto, modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            Sheet balN = wb.getSheet("BAL N");
            assertThat(balN.getRow(1).getCell(0).getStringCellValue()).isEqualTo("601000");
            assertThat(balN.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(150000.0);
            assertThat(balN.getRow(2).getCell(0).getStringCellValue()).isEqualTo("701000");
        }
    }

    @Test
    void fuseBalanceToExcel_rowsBeyond199AreIgnored() throws Exception {
        // 205 valid account rows in the source balance; addBalanceToModel only ever reads
        // source rows 1..199 (loop bound `row < 200`), so the 200th data row of "BAL N" must
        // remain untouched after the merge. This documents the current hard limit.
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= 205; i++) {
            rows.add(new Object[]{String.valueOf(600000 + i), "Compte " + i, (double) i});
        }
        String modelPath = WorkbookTestFixtures.modelWithSheet("BAL N", 210);
        String balancePath = WorkbookTestFixtures.balanceWorkbook(rows);
        EtatFinancierDto dto = newDto();

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(dto, modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            Sheet balN = wb.getSheet("BAL N");
            Row row200 = balN.getRow(200);
            assertThat(row200).isNotNull();
            Cell cell = row200.getCell(0);
            assertThat(cell == null || cell.getCellType() == CellType.BLANK).isTrue();
        }
    }

    @Test
    void fuseBalanceToExcel_whenModelHasNoBalNSheet_throwsNullPointerException() throws Exception {
        // Model without a "BAL N" sheet: documents that the current behavior is indeed a
        // NullPointerException (not an explicit business error) - not replaced by a clearer
        // exception here, as that would be a behavior change outside this plan's scope.
        String modelPath = WorkbookTestFixtures.modelWithSheet("Autre Feuille", 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbook(
                List.<Object[]>of(new Object[]{"601000", "Achats", 100.0}));
        EtatFinancierDto dto = newDto();

        assertThatThrownBy(() -> balanceMergeService.fuseBalanceToExcel(dto, modelPath, balancePath))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fuseBalanceToExcel_copiesBlankSourceCellsAsBlankDestinationCells() throws Exception {
        // The source balance has a BLANK cell in column D (account row "601000"); documents
        // that the corresponding cell in "BAL N" becomes BLANK after the merge (the
        // `default: destCell.setBlank();` branch of addBalanceToModel). The destination cell
        // (row 1, col 3) is seeded with a non-blank sentinel value so the assertion is only
        // satisfied if setBlank() actually ran - an already-blank destination cell would pass
        // the assertion regardless of whether that branch executed.
        String modelPath = WorkbookTestFixtures.modelWithSheetAndCellValue("BAL N", 10, 1, 3, "SENTINEL");
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithBlankCell("601000", 3);
        EtatFinancierDto dto = newDto();

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(dto, modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("BAL N").getRow(1).getCell(3).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void fuseBalanceToExcel_withTwoBalances_fillsBalNAndBalN1() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"), 10);
        String balanceN = WorkbookTestFixtures.balanceWorkbook(List.<Object[]>of(new Object[]{"601000", "Achats", 100.0}));
        String balanceN1 = WorkbookTestFixtures.balanceWorkbook(List.<Object[]>of(new Object[]{"601000", "Achats", 90.0}));
        EtatFinancierDto dto = newDto();

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(dto, modelPath, balanceN, balanceN1);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("BAL N").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(100.0);
            assertThat(wb.getSheet("BAL N-1").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(90.0);
        }
    }

    @Test
    void complement_threeArgOverload_writesToHardcodedPathNotEtatFinNPath() throws Exception {
        // Documents the current behavior (see Review Focus in the task brief): this overload
        // always writes to "public/dossier/test2.xlsx", never to etatFinNpath. Do NOT "fix" this
        // test to make it write to the right place - that would be a behavior change outside
        // this plan's scope (the method is scheduled for deletion in a later, separate plan).
        String etatFinNPath = WorkbookTestFixtures.modelWithSheet("FLUX DE TRESORERIE", 20);
        String etatFinN1Path = WorkbookTestFixtures.modelWithSheet("FLUX DE TRESORERIE", 20);
        long etatFinNPathOriginalLastModified = new File(etatFinNPath).lastModified();

        Files.deleteIfExists(Path.of("public/dossier/test2.xlsx"));

        balanceMergeService.complement(false, etatFinNPath, etatFinN1Path);

        assertThat(Files.exists(Path.of("public/dossier/test2.xlsx"))).isTrue();
        assertThat(new File(etatFinNPath).lastModified()).isEqualTo(etatFinNPathOriginalLastModified);
    }

    @Test
    void configExcelFile_nominalNumberField_writesValueToTargetCell() throws Exception {
        // Single nominal case: a ConfigModelExcelDto pointing at an existing EtatFinancierDto
        // getter (annee_n / NUMBER). Exhaustive coverage of the 6 TypeAttributIsConfig branches
        // is explicitly out of scope for this task.
        String excelPath = WorkbookTestFixtures.modelWithSheet("Sheet1", 10);
        EtatFinancierDto dto = newDto();
        dto.setExcelPath(excelPath);
        dto.setModelExcel_id(5L);

        ConfigModelExcelDto config = new ConfigModelExcelDto();
        config.setPage("Sheet1");
        config.setField("annee_n");
        config.setTypeAttributIsConfigConfig(TypeAttributIsConfig.NUMBER);
        config.setL_number(2);
        config.setC_number(3);

        ConfigModelExcelService configModelExcelService = mock(ConfigModelExcelService.class);
        when(configModelExcelService.getAll(argThat(f -> f != null && "ETATFINANCIER".equals(f.get("type_config")))))
                .thenReturn(List.of(config));
        when(configModelExcelService.getAll(argThat(f -> f != null && "ENTREPRISE".equals(f.get("type_config")))))
                .thenReturn(List.of());

        BalanceMergeServiceImpl service = new BalanceMergeServiceImpl(
                new HelperService(),
                null,
                configModelExcelService,
                mock(ActionaireEntrepriseService.class),
                mock(ActiviteEntrepriseService.class),
                mock(BanqueEntrepriseService.class),
                mock(DirigeantEntrepriseService.class),
                mock(GerantEntrepriseService.class));

        EtatFinancierDto result = service.configExcelFile(dto, dto.getEntreprise());

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("Sheet1").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(2025.0);
        }
    }
}
