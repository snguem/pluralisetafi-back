package pluralisconseil.sn.pluralisEtatFin.helpers.impls;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelTemplateService;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ExcelTemplateServiceImpl implements ExcelTemplateService {
    private final HelperService helperService;

    @Override
    public List<String> getExcelPageNames(String path) throws Exception {
        List<String> names=new ArrayList<>();
        if (!helperService.isExist(path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier est introuvable");

        FileInputStream fis = new FileInputStream(path);
        Workbook workbook = new XSSFWorkbook(fis);

        int numberOfSheets = workbook.getNumberOfSheets();

        for (int i = 0; i < numberOfSheets; i++) {
            String sheetName = workbook.getSheetName(i);
            names.add(sheetName);
        }

        fis.close();
        workbook.close();
        return names;
    }

    @Override
    public void setPartModelExcel(String path, int part) throws Exception {
        if (!helperService.isExist(path)) throw new FileNotFoundException("Le fichier est introuvable");

        Workbook workbook = new XSSFWorkbook(new FileInputStream(path));
        Sheet page = workbook.getSheet(("Note 13"));
        if (page!=null){
            Cell cell = page.getRow(7).getCell(8);
            cell.setCellValue(part);

//
            workbook.setForceFormulaRecalculation(true);
//
            FileOutputStream fileOut = new FileOutputStream(path);
            workbook.write(fileOut);

            fileOut.close();
            workbook.close();
        }

    }
}
