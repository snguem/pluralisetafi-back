package pluralisconseil.sn.pluralisEtatFin.helpers;

/** How a sheet is printed to PDF: the area to print (e.g. {@code $A$1:$H$38}, null for the whole sheet) and its orientation. */
public record PdfPageSettings(String printArea, boolean landscape) {
}
