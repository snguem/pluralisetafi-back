package pluralisconseil.sn.pluralisEtatFin.helpers.support;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds small in-memory POI workbooks for characterization tests and writes them out to
 * temporary {@code .xlsx} files on disk. Pure fixture construction: no ExcelService business
 * rules live here, only generic, reusable workbook shapes that characterization tests for
 * {@code ExcelService} (and its future extracted collaborators) can build on.
 */
public final class WorkbookTestFixtures {

    private static final int DEFAULT_COLUMN_COUNT = 8;

    private WorkbookTestFixtures() {
    }

    /**
     * Creates a workbook with a single sheet named {@code sheetName} containing {@code rowCount}
     * empty rows (columns A to H), written to a temporary file whose absolute path is returned.
     */
    public static String modelWithSheet(String sheetName, int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            for (int r = 0; r < rowCount; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < DEFAULT_COLUMN_COUNT; c++) {
                    row.createCell(c);
                }
            }
            return writeToTempFile(workbook, "modele_");
        }
    }

    /**
     * Creates a single-sheet workbook whose row 0 is a header row and whose following rows
     * contain {@code rows} (one {@code Object[]} per row, columns A onward), written to a
     * temporary file whose absolute path is returned.
     */
    public static String balanceWorkbook(List<Object[]> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Compte");
            header.createCell(1).setCellValue("Libelle");
            header.createCell(2).setCellValue("Solde");

            int rowIndex = 1;
            for (Object[] values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int c = 0; c < values.length; c++) {
                    setCellValue(row.createCell(c), values[c]);
                }
            }
            return writeToTempFile(workbook, "balance_");
        }
    }

    /**
     * Creates a single-row balance workbook for {@code accountNumber} whose cell at
     * {@code blankColumnIndex} is explicitly of type BLANK, so callers can exercise the
     * blank-source-cell branch of {@code ExcelService#addBalanceToModel}.
     */
    public static String balanceWorkbookWithBlankCell(String accountNumber, int blankColumnIndex) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Compte");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(accountNumber);
            row.createCell(blankColumnIndex, CellType.BLANK);

            return writeToTempFile(workbook, "balance_");
        }
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof String s) {
            cell.setCellValue(s);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private static String writeToTempFile(Workbook workbook, String prefix) throws IOException {
        Path tempFile = Files.createTempFile(prefix, ".xlsx");
        try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
            workbook.write(out);
        }
        return tempFile.toAbsolutePath().toString();
    }
}
