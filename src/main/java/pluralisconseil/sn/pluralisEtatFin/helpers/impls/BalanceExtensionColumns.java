package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import org.apache.poi.ss.usermodel.*;

/**
 * Adds, to a balance sheet, the helper columns the états financiers formulas rely on: the first
 * 1 to 5 characters of the account number ({@code GAUCHE(A2;n)} in a French Excel), in columns
 * I to M, headed "Ext 2", "Ext 3", "Ext 4", "Ext 5" and "Ext 1".
 *
 * <p>Only rows whose first cell is an account number (digits) get the formulas, so headings and
 * total lines of the balance are left alone.
 */
final class BalanceExtensionColumns {

    private static final int ACCOUNT_COLUMN = 0;
    private static final int FIRST_EXTENSION_COLUMN = 8;
    /** Number of leading characters kept by each extension column, left to right (I..M). */
    private static final int[] LENGTHS = {2, 3, 4, 5, 1};

    private BalanceExtensionColumns() {
    }

    static void add(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        addHeaders(sheet);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || !isAccountNumber(row.getCell(ACCOUNT_COLUMN), formatter)) continue;

            String account = "A" + (r + 1);
            for (int i = 0; i < LENGTHS.length; i++) {
                Cell cell = row.getCell(FIRST_EXTENSION_COLUMN + i);
                if (cell == null) cell = row.createCell(FIRST_EXTENSION_COLUMN + i);
                cell.setCellFormula("LEFT(" + account + "," + LENGTHS[i] + ")");
            }
        }
    }

    private static void addHeaders(Sheet sheet) {
        Row header = sheet.getRow(0) != null ? sheet.getRow(0) : sheet.createRow(0);
        Cell styleModel = header.getCell(FIRST_EXTENSION_COLUMN - 1);
        for (int i = 0; i < LENGTHS.length; i++) {
            Cell cell = header.getCell(FIRST_EXTENSION_COLUMN + i);
            if (cell == null) cell = header.createCell(FIRST_EXTENSION_COLUMN + i);
            cell.setCellValue("Ext " + LENGTHS[i]);
            if (styleModel != null) cell.setCellStyle(styleModel.getCellStyle());
        }
    }

    /** A whole number, or text made of digits - however the cell is displayed (e.g. "70616.00"). */
    private static boolean isAccountNumber(Cell cell, DataFormatter formatter) {
        if (cell == null) return false;
        if (cell.getCellType() == CellType.NUMERIC)
            return cell.getNumericCellValue() >= 0 && cell.getNumericCellValue() == Math.rint(cell.getNumericCellValue());
        return formatter.formatCellValue(cell).trim().matches("\\d+");
    }
}
