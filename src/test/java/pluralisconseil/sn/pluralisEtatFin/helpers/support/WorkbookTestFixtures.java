package pluralisconseil.sn.pluralisEtatFin.helpers.support;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
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
 * Builds small in-memory POI workbooks for the excel services tests and writes them out to
 * temporary {@code .xlsx} files on disk. Pure fixture construction: no business
 * rules live here, only generic, reusable workbook shapes that tests of
 * {@code BalanceMergeService} and the other excel services can build on.
 */
public final class WorkbookTestFixtures {

    private static final int DEFAULT_COLUMN_COUNT = 9;

    private WorkbookTestFixtures() {
    }

    /**
     * Creates a workbook with a single sheet named {@code sheetName} containing {@code rowCount}
     * empty rows (columns A to I), written to a temporary file whose absolute path is returned.
     */
    public static String modelWithSheet(String sheetName, int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            createEmptySheet(workbook, sheetName, rowCount);
            return writeToTempFile(workbook, "modele_");
        }
    }

    /**
     * Like {@link #modelWithSheet(String, int)}, but pre-seeds the cell at
     * ({@code seedRow}, {@code seedCol}) with {@code seedValue} instead of leaving it blank.
     * Useful for characterization tests that must prove a merge actually overwrote a
     * destination cell, rather than merely leaving an already-blank cell blank.
     */
    public static String modelWithSheetAndCellValue(String sheetName, int rowCount, int seedRow, int seedCol, String seedValue) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = createEmptySheet(workbook, sheetName, rowCount);
            sheet.getRow(seedRow).getCell(seedCol).setCellValue(seedValue);
            return writeToTempFile(workbook, "modele_");
        }
    }

    /** Like {@link #modelWithSheets(List, int)}, with a sentinel value seeded in one cell of {@code seedSheet}. */
    public static String modelWithSheetsAndCellValue(List<String> sheetNames, int rowCount, String seedSheet, int seedRow, int seedCol, String seedValue) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (String name : sheetNames) {
                createEmptySheet(workbook, name, rowCount);
            }
            workbook.getSheet(seedSheet).getRow(seedRow).getCell(seedCol).setCellValue(seedValue);
            return writeToTempFile(workbook, "modele_");
        }
    }

    /**
     * Creates a workbook with one empty sheet per name in {@code sheetNames}, in the given
     * order, written to a temporary file whose absolute path is returned. Useful for
     * characterization tests that only need to assert on sheet names/order, not sheet contents.
     */
    public static String modelWithSheets(List<String> sheetNames) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (String sheetName : sheetNames) {
                workbook.createSheet(sheetName);
            }
            return writeToTempFile(workbook, "modele_");
        }
    }

    /**
     * Like {@link #modelWithSheets(List)}, but each sheet is pre-populated with {@code rowCount}
     * empty rows (columns A to I), so merges that read/write existing rows (e.g.
     * {@code BalanceMergeServiceImpl#addBalanceToModel}) have somewhere to write into.
     */
    public static String modelWithSheets(List<String> sheetNames, int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (String sheetName : sheetNames) {
                createEmptySheet(workbook, sheetName, rowCount);
            }
            return writeToTempFile(workbook, "modele_");
        }
    }

    private static Sheet createEmptySheet(Workbook workbook, String sheetName, int rowCount) {
        Sheet sheet = workbook.createSheet(sheetName);
        for (int r = 0; r < rowCount; r++) {
            Row row = sheet.createRow(r);
            for (int c = 0; c < DEFAULT_COLUMN_COUNT; c++) {
                row.createCell(c);
            }
        }
        return sheet;
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

    /** Same as {@link #balanceWorkbook} but with one sheet per entry of {@code sheets}. */
    public static String balanceWorkbookWithSheets(List<List<Object[]>> sheets) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            int sheetNumber = 1;
            for (List<Object[]> rows : sheets) {
                Sheet sheet = workbook.createSheet("Balance " + sheetNumber++);
                sheet.createRow(0).createCell(0).setCellValue("Compte");
                int rowIndex = 1;
                for (Object[] values : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int c = 0; c < values.length; c++) {
                        setCellValue(row.createCell(c), values[c]);
                    }
                }
            }
            return writeToTempFile(workbook, "balance_");
        }
    }

    /**
     * Two-sheet balances file whose first sheet has a bold header, a 6000-wide first column and
     * one merged region.
     */
    public static String balanceWorkbookWithStyledSheets() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);

            for (String name : List.of("Balance N", "Balance N-1")) {
                Sheet sheet = workbook.createSheet(name);
                Cell header = sheet.createRow(0).createCell(0);
                header.setCellValue("Compte");
                header.setCellStyle(headerStyle);
                sheet.createRow(1).createCell(0).setCellValue("601000");
                sheet.setColumnWidth(0, 6000);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 2));
            }
            return writeToTempFile(workbook, "balance_");
        }
    }

    /** A balances file as the import expects it: the same rows on a "N" sheet and a "N-1" sheet. */
    public static String balanceWorkbookWithTwoSheets(List<Object[]> rows) throws IOException {
        return balanceWorkbookWithSheets(List.of(rows, rows));
    }

    /**
     * Creates a single-row balance workbook for {@code accountNumber} whose cell at
     * {@code blankColumnIndex} is explicitly of type BLANK, so callers can exercise the
     * blank-source-cell branch of {@code BalanceMergeServiceImpl}'s sheet replacement.
     */
    public static String balanceWorkbookWithBlankCell(String accountNumber, int blankColumnIndex) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Balance N-1");
            Sheet sheet = workbook.createSheet("Balance");
            workbook.setSheetOrder("Balance", 0);

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
