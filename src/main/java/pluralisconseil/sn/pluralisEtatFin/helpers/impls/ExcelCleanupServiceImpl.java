package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelCleanupService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ExcelCleanupServiceImpl implements ExcelCleanupService {

    @Override
    public void nettoyerEtRecupererExcel(String path) throws IOException {

        // 1. Ouvrir le fichier existant
        FileInputStream fis = new FileInputStream(path);
        Workbook workbook = new XSSFWorkbook(fis);

        int numberOfSheets = workbook.getNumberOfSheets();
        List<Integer> indicesASupprimer = new ArrayList<>();

        for (int i = 0; i < numberOfSheets; i++) {
            String sheetName = workbook.getSheetName(i);

            // Condition : Si le nom contient "evaluation" (insensible à la casse si besoin)
            if (sheetName.toLowerCase().contains("evaluation warning")) {
                indicesASupprimer.add(i);
            }
        }

        fis.close();

        for (int i = indicesASupprimer.size() - 1; i >= 0; i--) {
            int indexToDelete = indicesASupprimer.get(i);
            workbook.removeSheetAt(indexToDelete);
        }


//
        workbook.setForceFormulaRecalculation(true);
//

        // On ouvre un flux vers le fichier de destination
        FileOutputStream fos = new FileOutputStream(path);

        // On écrit le contenu du workbook dans ce flux
        workbook.write(fos);

        // On ferme le flux d'écriture et le workbook
        fos.close();
        workbook.close();

    }
}
