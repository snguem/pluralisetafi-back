package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pluralisconseil.sn.pluralisEtatFin.helpers.support.WorkbookTestFixtures;

import java.io.FileInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization test for {@link ExcelCleanupServiceImpl}, extracted verbatim from
 * {@code ExcelService#nettoyerEtRecupererExcel}.
 */
class ExcelCleanupServiceImplTest {

    private final ExcelCleanupServiceImpl service = new ExcelCleanupServiceImpl();

    @Test
    void nettoyerEtRecupererExcel_removesEvaluationWarningSheetsOnly() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheets(
            List.of("BAL N", "Evaluation Warning", "Note 13"));
        service.nettoyerEtRecupererExcel(path);
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(path))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheet("Evaluation Warning")).isNull();
            assertThat(wb.getSheet("BAL N")).isNotNull();
        }
    }
}
