package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the whole content of a sheet of one workbook by a sheet of another workbook.
 *
 * <p>POI cannot move a sheet between workbooks, so the target sheet is emptied and rebuilt from
 * the source: cells, styles, merged regions, row heights and column widths. The target sheet
 * keeps its name and position, so the references other sheets and the print setup hold to it
 * stay valid.
 */
final class SheetContentReplacer {

    private SheetContentReplacer() {
    }

    static void replace(Sheet target, Sheet source) {
        clear(target);

        Workbook targetWorkbook = target.getWorkbook();
        Map<Short, CellStyle> styles = new HashMap<>();

        int lastColumn = 0;
        for (Row sourceRow : source) {
            Row targetRow = target.createRow(sourceRow.getRowNum());
            if (sourceRow.getZeroHeight()) targetRow.setZeroHeight(true);
            else if (sourceRow.getHeightInPoints() > 0)
                targetRow.setHeightInPoints(sourceRow.getHeightInPoints());

            for (Cell sourceCell : sourceRow) {
                Cell targetCell = targetRow.createCell(sourceCell.getColumnIndex());
                lastColumn = Math.max(lastColumn, sourceCell.getColumnIndex());
                copyStyle(sourceCell, targetCell, targetWorkbook, styles);
                copyValue(sourceCell, targetCell);
            }
        }

        for (int col = 0; col <= lastColumn; col++) {
            target.setColumnWidth(col, source.getColumnWidth(col));
            target.setColumnHidden(col, source.isColumnHidden(col));
        }
        for (int i = 0; i < source.getNumMergedRegions(); i++) {
            CellRangeAddress region = source.getMergedRegion(i);
            target.addMergedRegion(new CellRangeAddress(
                    region.getFirstRow(), region.getLastRow(), region.getFirstColumn(), region.getLastColumn()));
        }
    }

    private static void clear(Sheet sheet) {
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            sheet.removeMergedRegion(i);
        }
        for (int i = sheet.getLastRowNum(); i >= sheet.getFirstRowNum() && i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) sheet.removeRow(row);
        }
    }

    private static void copyStyle(Cell source, Cell target, Workbook targetWorkbook, Map<Short, CellStyle> styles) {
        CellStyle sourceStyle = source.getCellStyle();
        if (sourceStyle == null) return;
        // one style per source style: a workbook can only hold a limited number of them
        target.setCellStyle(styles.computeIfAbsent(sourceStyle.getIndex(), k -> {
            CellStyle style = targetWorkbook.createCellStyle();
            style.cloneStyleFrom(sourceStyle);
            return style;
        }));
    }

    private static void copyValue(Cell source, Cell target) {
        switch (source.getCellType()) {
            case STRING -> target.setCellValue(source.getStringCellValue());
            case NUMERIC -> target.setCellValue(source.getNumericCellValue());
            case BOOLEAN -> target.setCellValue(source.getBooleanCellValue());
            case ERROR -> target.setCellErrorValue(source.getErrorCellValue());
            case FORMULA -> copyFormula(source, target);
            default -> target.setBlank();
        }
    }

    /**
     * Keeps the formula; if it can't live in the target workbook (e.g. it points to a sheet that
     * only exists in the balance file), falls back on the value it had last been computed to.
     */
    private static void copyFormula(Cell source, Cell target) {
        try {
            target.setCellFormula(source.getCellFormula());
        } catch (RuntimeException ex) {
            switch (source.getCachedFormulaResultType()) {
                case STRING -> target.setCellValue(source.getStringCellValue());
                case NUMERIC -> target.setCellValue(source.getNumericCellValue());
                case BOOLEAN -> target.setCellValue(source.getBooleanCellValue());
                default -> target.setBlank();
            }
        }
    }
}
