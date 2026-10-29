package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.util.List;

public interface ExcelToPdfConverter {

    /** The PDF of the listed sheets, printed with the settings of the model {@code modelId} (null: the default rules only). */
    byte[] toPdf(String excelPath, List<String> pages, Long modelId);

    default byte[] toPdf(String excelPath, List<String> pages) {
        return toPdf(excelPath, pages, null);
    }
}
