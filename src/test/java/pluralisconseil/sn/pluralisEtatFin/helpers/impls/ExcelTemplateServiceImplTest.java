package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.helpers.support.WorkbookTestFixtures;

import java.io.FileInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests for {@link ExcelTemplateServiceImpl}, extracted verbatim from
 * {@code ExcelService#getExcelPageNames} and {@code ExcelService#setPartModelExcel}.
 */
class ExcelTemplateServiceImplTest {

    private final ExcelTemplateServiceImpl service = new ExcelTemplateServiceImpl(new HelperService());

    @Test
    void getExcelPageNames_returnsAllSheetNamesInOrder() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1", "Note 13"));
        assertThat(service.getExcelPageNames(path)).containsExactly("BAL N", "BAL N-1", "Note 13");
    }

    @Test
    void setPartModelExcel_writesPartIntoNote13Row7Col8() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheet("Note 13", 10);
        service.setPartModelExcel(path, 3);
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(path))) {
            assertThat(wb.getSheet("Note 13").getRow(7).getCell(8).getNumericCellValue()).isEqualTo(3.0);
        }
    }

    @Test
    void setPartModelExcel_noOpsWhenNote13Missing() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheet("BAL N", 10);
        service.setPartModelExcel(path, 3); // ne doit pas lever d'exception
    }
}
