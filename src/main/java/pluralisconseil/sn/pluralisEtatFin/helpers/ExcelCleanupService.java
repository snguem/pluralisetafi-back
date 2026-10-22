package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.io.IOException;

public interface ExcelCleanupService {
    void nettoyerEtRecupererExcel(String path) throws IOException;
}
