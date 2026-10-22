package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.util.List;

public interface ExcelToPdfConverter {
    byte[] toPdf(String excelPath, List<String> pages);
}
