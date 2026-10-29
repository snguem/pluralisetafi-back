package pluralisconseil.sn.pluralisEtatFin.helpers;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;

import java.util.Optional;
import java.util.TreeSet;

/**
 * Print settings computed from what a sheet really holds: the area is the rectangle that contains
 * every filled cell, and the orientation is the one that lets that rectangle fill an A4 page the most.
 */
public final class SheetContentFit {

    /** A4 in points. */
    private static final double A4_SHORT = 595.0;
    private static final double A4_LONG = 842.0;

    /**
     * A sheet can hold helper data far away from its table (e.g. in the columns at the very end). Content stops at the
     * first gap larger than these, so such cells do not stretch the area over hundreds of pages.
     */
    private static final int MAX_EMPTY_COLUMNS = 15;
    private static final int MAX_EMPTY_ROWS = 25;

    /**
     * A helper row can repeat a value every few columns across the whole sheet (thousands of cells). A row that spans
     * more than this many columns while filling less than {@link #MIN_DENSITY} of them is ignored.
     */
    private static final int MAX_ROW_SPAN = 100;
    private static final double MIN_DENSITY = 0.25;

    private SheetContentFit() {
    }

    /** Empty when the sheet holds nothing to print. */
    public static Optional<PdfPageSettings> forSheet(Sheet sheet) {
        var filledRows = new TreeSet<Integer>();
        var filledCols = new TreeSet<Integer>();
        for (Row row : sheet) {
            var cols = new TreeSet<Integer>();
            for (Cell cell : row) {
                if (isFilled(cell)) cols.add(cell.getColumnIndex());
            }
            if (cols.isEmpty() || isHelperRow(cols)) continue;
            filledRows.add(row.getRowNum());
            filledCols.addAll(cols);
        }
        if (filledRows.isEmpty()) return Optional.empty();
        int lastRow = contiguousEnd(filledRows, MAX_EMPTY_ROWS);
        int lastCol = contiguousEnd(filledCols, MAX_EMPTY_COLUMNS);

        // a merged block that starts on a filled cell also belongs to the content
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            // the blocks of an ignored helper row do not count either
            if (!filledRows.contains(region.getFirstRow())) continue;
            var first = sheet.getRow(region.getFirstRow());
            var cell = first == null ? null : first.getCell(region.getFirstColumn());
            if (!isFilled(cell)) continue;
            lastRow = Math.max(lastRow, region.getLastRow());
            lastCol = Math.max(lastCol, region.getLastColumn());
        }

        double width = 0, height = 0;
        for (int col = 0; col <= lastCol; col++) {
            width += sheet.isColumnHidden(col) ? 0 : sheet.getColumnWidthInPixels(col) * 0.75; // pixels to points
        }
        for (int rowNum = 0; rowNum <= lastRow; rowNum++) {
            var row = sheet.getRow(rowNum);
            if (row != null && row.getZeroHeight()) continue;
            height += row != null ? row.getHeightInPoints() : sheet.getDefaultRowHeightInPoints();
        }

        // the scale that fits the area on the page, for each orientation: the bigger one fills the page more
        double portrait = Math.min(A4_SHORT / width, A4_LONG / height);
        double landscape = Math.min(A4_LONG / width, A4_SHORT / height);

        var area = "$A$1:$" + CellReference.convertNumToColString(lastCol) + "$" + (lastRow + 1);
        return Optional.of(new PdfPageSettings(area, landscape > portrait));
    }

    private static boolean isHelperRow(TreeSet<Integer> cols) {
        int span = cols.last() - cols.first() + 1;
        return span > MAX_ROW_SPAN && (double) cols.size() / span < MIN_DENSITY;
    }

    /** The last index reached from the first one without crossing more than {@code maxGap} empty indexes in a row. */
    private static int contiguousEnd(TreeSet<Integer> filled, int maxGap) {
        int end = filled.first();
        for (int index : filled) {
            if (index - end - 1 > maxGap) break;
            end = index;
        }
        return end;
    }

    private static boolean isFilled(Cell cell) {
        if (cell == null) return false;
        var type = cell.getCellType();
        if (type == CellType.BLANK || type == CellType.ERROR) return false;
        if (type == CellType.STRING) return !cell.getStringCellValue().isBlank();
        return true; // numbers, booleans and formulas
    }
}
