package pluralisconseil.sn.pluralisEtatFin.helpers;

/**
 * A generated PDF and how it was made: how many sheets it holds and how many of them were printed with
 * the settings of the model (the others follow the default rules).
 */
public record PdfExport(byte[] pdf, int sheets, int sheetsWithModelSettings) {
}
