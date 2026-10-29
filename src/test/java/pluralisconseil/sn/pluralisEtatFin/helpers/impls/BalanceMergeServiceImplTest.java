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
import pluralisconseil.sn.pluralisEtatFin.exceptions.BadRequestException;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.helpers.support.WorkbookTestFixtures;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActionaireEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActiviteEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.BanqueEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ConfigModelExcelService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DirigeantEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.GerantEntrepriseService;

import java.io.FileInputStream;
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
 * sheet) using workbooks
 * built entirely in memory with Apache POI, rather than relying on the project's binary .xlsx
 * fixtures.
 */
class BalanceMergeServiceImplTest {

    // Only helperService.isExist(...) is exercised by addBalanceToModel/fuseBalanceToExcel; the
    // other collaborators of BalanceMergeServiceImpl are not used on these code paths, so they
    // are left null rather than mocked.
    private final BalanceMergeServiceImpl balanceMergeService =
            new BalanceMergeServiceImpl(new HelperService(), null, null, null, null, null, null);

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
        String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"), 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithTwoSheets(List.of(
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
    void fuseBalanceToExcel_copiesTheWholeSheetWithoutRowLimit() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= 305; i++) {
            rows.add(new Object[]{String.valueOf(600000 + i), "Compte " + i, (double) i});
        }
        String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"), 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithTwoSheets(rows);

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(newDto(), modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("BAL N").getLastRowNum()).isEqualTo(305);
            assertThat(wb.getSheet("BAL N").getRow(305).getCell(0).getStringCellValue()).isEqualTo("600305");
        }
    }

    @Test
    void fuseBalanceToExcel_replacesTheSheetEntirely_headerStylesWidthsAndMergesIncluded() throws Exception {
        // model sheets hold leftovers (sentinel text, a merged region) that must NOT survive
        String modelPath = WorkbookTestFixtures.modelWithSheetsAndCellValue(List.of("BAL N", "BAL N-1"), 10, "BAL N", 8, 5, "LEFTOVER");
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithStyledSheets();

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(newDto(), modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            Sheet balN = wb.getSheet("BAL N");
            assertThat(wb.getSheetName(0)).isEqualTo("BAL N");
            assertThat(balN.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Compte");
            assertThat(((org.apache.poi.xssf.usermodel.XSSFCellStyle) balN.getRow(0).getCell(0).getCellStyle()).getFont().getBold()).isTrue();
            assertThat(balN.getColumnWidth(0)).isEqualTo(6000);
            assertThat(balN.getNumMergedRegions()).isEqualTo(1);
            Row leftoverRow = balN.getRow(8);
            assertThat(leftoverRow == null || leftoverRow.getCell(5) == null).isTrue();
        }
    }

    @Test
    void fuseBalanceToExcel_addsExtensionColumnsWithLeftFormulas_onBothSheets() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"), 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithTwoSheets(List.of(
                new Object[]{"10120", "Capital", 300000.0},
                new Object[]{"TOTAL", "Total", 300000.0}));

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(newDto(), modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();
            var formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            for (String name : List.of("BAL N", "BAL N-1")) {
                Sheet sheet = wb.getSheet(name);
                // I..M = Ext 2, Ext 3, Ext 4, Ext 5, Ext 1
                String[] headers = {"Ext 2", "Ext 3", "Ext 4", "Ext 5", "Ext 1"};
                String[] values = {"10", "101", "1012", "10120", "1"};
                for (int i = 0; i < 5; i++) {
                    assertThat(sheet.getRow(0).getCell(8 + i).getStringCellValue()).isEqualTo(headers[i]);
                    assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(8 + i), evaluator)).isEqualTo(values[i]);
                }
                assertThat(sheet.getRow(1).getCell(8).getCellFormula()).isEqualTo("LEFT(A2,2)");
                // a non-account line (here "TOTAL") gets no formula
                assertThat(sheet.getRow(2).getCell(8)).isNull();
            }
        }
    }

    @Test
    void fuseBalanceToExcel_whenModelHasNoBalNSheet_isRejected() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheet("Autre Feuille", 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithTwoSheets(
                List.<Object[]>of(new Object[]{"601000", "Achats", 100.0}));
        EtatFinancierDto dto = newDto();

        assertThatThrownBy(() -> balanceMergeService.fuseBalanceToExcel(dto, modelPath, balancePath))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("BAL N");
    }

    @Test
    void fuseBalanceToExcel_copiesBlankSourceCellsAsBlankDestinationCells() throws Exception {
        // The source balance has a BLANK cell in column D (account row "601000"); documents
        // that the corresponding cell in "BAL N" becomes BLANK after the merge (the
        // `default: destCell.setBlank();` branch of addBalanceToModel). The destination cell
        // (row 1, col 3) is seeded with a non-blank sentinel value so the assertion is only
        // satisfied if setBlank() actually ran - an already-blank destination cell would pass
        // the assertion regardless of whether that branch executed.
        String modelPath = WorkbookTestFixtures.modelWithSheetsAndCellValue(List.of("BAL N", "BAL N-1"), 10, "BAL N", 1, 3, "SENTINEL");
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
    void fuseBalanceToExcel_singleFileWithTwoSheets_firstSheetFillsBalNSecondFillsBalN1() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"), 10);
        String balances = WorkbookTestFixtures.balanceWorkbookWithSheets(List.of(
                List.<Object[]>of(new Object[]{"601000", "Achats", 100.0}),
                List.<Object[]>of(new Object[]{"601000", "Achats", 90.0})));

        EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(newDto(), modelPath, balances);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("BAL N").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(100.0);
            assertThat(wb.getSheet("BAL N-1").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(90.0);
        }
    }

    @Test
    void fuseBalanceToExcel_singleFileWithOneSheet_isRejectedAndWritesNothing() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"), 10);
        String balances = WorkbookTestFixtures.balanceWorkbook(List.<Object[]>of(new Object[]{"601000", "Achats", 100.0}));
        EtatFinancierDto dto = newDto();

        assertThatThrownBy(() -> balanceMergeService.fuseBalanceToExcel(dto, modelPath, balances))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2 feuilles");
        assertThat(dto.getExcelPath()).isNull();
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
