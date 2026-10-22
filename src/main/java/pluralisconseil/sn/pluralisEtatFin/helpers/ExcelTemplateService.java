package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.util.List;

public interface ExcelTemplateService {
    List<String> getExcelPageNames(String path) throws Exception;
    void setPartModelExcel(String path, int part) throws Exception;
}
