package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.util.List;

public interface ExcelToPdfConverter {

    /** The PDF of the listed sheets, printed with the settings of the model {@code modelId} (null: the default rules only); null when the conversion failed. */
    PdfExport export(String excelPath, List<String> pages, Long modelId);

    default byte[] toPdf(String excelPath, List<String> pages, Long modelId) {
        var result = export(excelPath, pages, modelId);
        return result == null ? null : result.pdf();
    }

    default byte[] toPdf(String excelPath, List<String> pages) {
        return toPdf(excelPath, pages, null);
    }
}
