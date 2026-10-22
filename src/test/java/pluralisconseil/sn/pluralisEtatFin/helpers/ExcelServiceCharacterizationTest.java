package pluralisconseil.sn.pluralisEtatFin.helpers;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseSubstituteDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.helpers.support.WorkbookTestFixtures;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Characterization tests for {@link ExcelService#fuseBalanceToExcel}. These pin down the
 * CURRENT behavior of the method (including its quirks, e.g. the 200-row limit and the
 * NullPointerException when the model lacks a "BAL N" sheet) using workbooks built entirely
 * in memory with Apache POI, rather than relying on the project's binary .xlsx fixtures.
 *
 * <p>This test is expected to be moved (not rewritten) once {@code BalanceMergeService} is
 * extracted from {@code ExcelService} in a later task of this plan.
 */
class ExcelServiceCharacterizationTest {

    // Only helperService.isExist(...) is exercised by addBalanceToModel/fuseBalanceToExcel;
    // the other collaborators of ExcelService are not used on this code path, so they are left
    // null rather than mocked.
    private final ExcelService excelService =
            new ExcelService(new HelperService(), null, null, null, null, null, null, null);

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

        EtatFinancierDto result = excelService.fuseBalanceToExcel(dto, modelPath, balancePath);

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

        EtatFinancierDto result = excelService.fuseBalanceToExcel(dto, modelPath, balancePath);

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

        assertThatThrownBy(() -> excelService.fuseBalanceToExcel(dto, modelPath, balancePath))
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

        EtatFinancierDto result = excelService.fuseBalanceToExcel(dto, modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("BAL N").getRow(1).getCell(3).getCellType()).isEqualTo(CellType.BLANK);
        }
    }
}
