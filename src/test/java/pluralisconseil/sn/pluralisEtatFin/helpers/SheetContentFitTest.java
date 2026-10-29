package pluralisconseil.sn.pluralisEtatFin.helpers;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests on in-memory workbooks: no Spring context, no database. */
class SheetContentFitTest {

    private static Sheet sheet() {
        return new XSSFWorkbook().createSheet("S");
    }

    private static void fill(Sheet sheet, int rows, int cols) {
        for (int r = 0; r < rows; r++) {
            var row = sheet.createRow(r);
            for (int c = 0; c < cols; c++) row.createCell(c).setCellValue("x");
        }
    }

    @Test
    void area_isTheRectangleOfTheFilledCells() {
        var sheet = sheet();
        fill(sheet, 40, 6);

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$F$40");
    }

    @Test
    void aTallNarrowSheetIsPrintedInPortrait() {
        var sheet = sheet();
        fill(sheet, 60, 3);

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().landscape()).isFalse();
    }

    @Test
    void aWideShortSheetIsPrintedInLandscape() {
        var sheet = sheet();
        fill(sheet, 8, 14);
        for (int c = 0; c < 14; c++) sheet.setColumnWidth(c, 18 * 256);

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().landscape()).isTrue();
    }

    @Test
    void blankCellsDoNotExtendTheArea() {
        var sheet = sheet();
        fill(sheet, 10, 4);
        sheet.getRow(3).createCell(30); // formatted but empty, far to the right
        sheet.createRow(80).createCell(0).setCellValue("   "); // spaces only

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$D$10");
    }

    @Test
    void helperDataFarFromTheTableDoesNotStretchTheArea() {
        var sheet = sheet();
        fill(sheet, 31, 7);
        // helper cells repeated far to the right and far below, as a model can hold them
        for (int col = 63; col < 400; col += 8) sheet.getRow(5).createCell(col).setCellValue("aide");
        sheet.createRow(500).createCell(0).setCellValue("aide");

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$G$31");
    }

    @Test
    void aHelperRowRepeatedAcrossTheWholeSheetIsIgnored() {
        var sheet = sheet();
        fill(sheet, 31, 7);
        // one value every 8 columns, from the table to the last column of the sheet: no gap larger than the limit
        for (int col = 8; col < 16000; col += 8) sheet.getRow(5).createCell(col).setCellValue("aide");

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$G$31");
    }

    @Test
    void mergedBlocksOfAHelperRowDoNotStretchTheArea() {
        var sheet = sheet();
        fill(sheet, 31, 7);
        for (int col = 8; col < 16000; col += 8) {
            sheet.getRow(5).createCell(col).setCellValue("aide");
            sheet.addMergedRegion(new CellRangeAddress(5, 5, col, col + 7));
        }

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$G$31");
    }

    @Test
    void aWideTableThatIsFullyFilledIsKept() {
        var sheet = sheet();
        fill(sheet, 20, 120);

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$DP$20");
    }

    @Test
    void smallGapsInsideTheTableAreKept() {
        var sheet = sheet();
        fill(sheet, 10, 3);
        sheet.createRow(20).createCell(5).setCellValue("total"); // 9 empty rows and 2 empty columns away

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$F$21");
    }

    @Test
    void aMergedBlockStartingOnAFilledCellExtendsTheArea() {
        var sheet = sheet();
        fill(sheet, 5, 3);
        sheet.getRow(0).createCell(0).setCellValue("Titre");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        assertThat(SheetContentFit.forSheet(sheet).orElseThrow().printArea()).isEqualTo("$A$1:$H$5");
    }

    @Test
    void anEmptySheetHasNothingToPrint() {
        assertThat(SheetContentFit.forSheet(sheet())).isEmpty();
    }
}
