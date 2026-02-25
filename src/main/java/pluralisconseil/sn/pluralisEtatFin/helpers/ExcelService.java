package pluralisconseil.sn.pluralisEtatFin.helpers;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.models.*;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeAttributIsConfig;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;
import pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.helpers.json.JsonMatriceModel;
import pluralisconseil.sn.pluralisEtatFin.helpers.json.JsonModel;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.*;


import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

@RequiredArgsConstructor
@Service
public class ExcelService {
    private final HelperService helperService;
    private final JsonService jsonService;
    private final ConfigModelExcelService configModelExcelService;
    private final ActionaireEntrepriseService actionaireEntrepriseService;
    private final ActiviteEntrepriseService activiteEntrepriseService;
    private final BanqueEntrepriseService banqueEntrepriseService;
    private final DirigeantEntrepriseService dirigeantEntrepriseService;
    private final GerantEntrepriseService gerantEntrepriseService;


    public EtatFinancierDto addBalanceToModel(EtatFinancierDto dto, String sheetModel, String model_path, String balance_path) throws IOException {
            if (!helperService.isExist(model_path))
                throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier du model est introuvable");

            Workbook balance_n = new XSSFWorkbook(new FileInputStream(balance_path));
            // chargement du model
            Workbook workbook = new XSSFWorkbook(new FileInputStream(model_path));

            // Accéder à la première feuille
            Sheet balN = workbook.getSheet(sheetModel);
            Sheet bal_nSheet = balance_n.getSheetAt(0);


            DataFormatter formatter = new DataFormatter();

            int truRow = 1;
            for (int row = 1; row < 200; row++) {
                Row sourceRow = bal_nSheet.getRow(row);

                if (sourceRow == null) {
                    continue;
                }

                Cell cellAccount = sourceRow.getCell(0);
                String accountVal = formatter.formatCellValue(cellAccount);

                if (accountVal == null || !accountVal.matches("-?\\d+(\\.\\d+)?")) {
                     continue;
                }

                Row destRow = balN.getRow(truRow);

                // Accéder à la cellule de la colonne 0 (colonne A)
                for (int col = 0; col < 8; col++) {
                    Cell srcCell = sourceRow.getCell(col);

                    if (srcCell != null) {

                        Cell destCell = destRow.getCell(col);

                        switch (srcCell.getCellType()) {
                            case STRING:
                                destCell.setCellValue(srcCell.getStringCellValue());
                                break;
                            case NUMERIC:
                                destCell.setCellValue(srcCell.getNumericCellValue());
                                break;
                            case BOOLEAN:
                                destCell.setCellValue(srcCell.getBooleanCellValue());
                                break;
                            case FORMULA:
                                destCell.setCellFormula(srcCell.getCellFormula());
                                break;
                            default:
                                destCell.setBlank();
                        }
                    }

                }

                truRow ++;
            }

//          path excel
            String pathExcel = model_path.replace("modele_", dto.getId()+"_etat_financier_"+dto.getEntreprise().getNameEntreprise()+dto.getAnnee_n()+"_");
            pathExcel = pathExcel.replace(" ", "_");
            dto.setExcelPath(pathExcel);
//
            workbook.setForceFormulaRecalculation(true);
//
            FileOutputStream fileOut = new FileOutputStream(pathExcel);
            workbook.write(fileOut);
            fileOut.close();

            balance_n.close();
            workbook.close();
            return dto;

    }

    public EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path) throws Exception {
        var dto_ = addBalanceToModel(dto, "BAL N", model_path, balance_n_path);
        if (dto_!=null)
            return dto_;
        return dto;
    }

    public EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path, String balance_n_1_path) throws Exception {
        addBalanceToModel(dto, "BAL N", model_path, balance_n_path);
        addBalanceToModel(dto, "BAL N-1", dto.getExcelPath(), balance_n_1_path);

        return dto;

    }

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
    
    public EtatFinancierDto configExcelFile(EtatFinancierDto dto, EntrepriseSubstituteDto entrepriseDto) throws Exception{
            if (!helperService.isExist(dto.getExcelPath())) throw new FileNotFoundException("Le fichier est introuvable");

            // chargement du model
            Workbook workbook = new XSSFWorkbook(new FileInputStream(dto.getExcelPath()));
//            chargement des configs
//            List<Map<String, Object>> configsModel1 = SetModelPageValues.model1ConfigEntreprise();
            Map<String, String> filter=new HashMap<>();
            filter.put("model_excel", ""+ dto.getModelExcel_id());
            filter.put("type_config", TypeConfig.ETATFINANCIER.name());
            List<ConfigModelExcelDto> configEtats = configModelExcelService.getAll(filter);
            filter.replace("type_config", TypeConfig.ENTREPRISE.name());
            List<ConfigModelExcelDto> configEntreprise = configModelExcelService.getAll(filter);

            for (ConfigModelExcelDto configDto: configEtats){
                Sheet page = workbook.getSheet(configDto.getPage());
                if (page!=null){
                    configDto.setL_number(configDto.getL_number()-1);
                    configDto.setC_number(configDto.getC_number()-1);
                    if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.BOOLEAN)) {

                        Cell cell = page.getRow(configDto.getL_number()).getCell(configDto.getC_number());
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);

                        Method getterMethod = dto.getClass().getMethod(getterName);
                        Object val = getterMethod.invoke(dto);

                        if (val!=null && (Boolean) val)
                            cell.setCellValue("X");
                    }else if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.NUMBER)) {

                        Cell cell = page.getRow(configDto.getL_number()).getCell(configDto.getC_number());
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);

                        Method getterMethod = dto.getClass().getMethod(getterName);
                        Object val = getterMethod.invoke(dto);
                        if (val!=null)
                            cell.setCellValue((Integer) val);

                    }else if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.LISTNUMBER)) {
                        Cell cell = null;
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);
                        Method getterMethod = dto.getClass().getMethod(getterName);
                        var value = getterMethod.invoke(dto);
                        if (value!=null){
                            for (int i = 0; i < String.valueOf(value).toCharArray().length; i++) {
                                var num = String.valueOf(value).toCharArray()[i];
                                cell = page.getRow(configDto.getC_number()+i).getCell(configDto.getL_number());
                                cell.setCellValue(Integer.parseInt(num+""));
                            }
                        }
                    } else if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.FUSE)) {
                        Cell cell = page.getRow(configDto.getL_number()).getCell(configDto.getC_number());
                        List<String> fields_to_concat = Arrays.stream(configDto.getClass_fields_if_list().split(",")).toList();
                        String value_concated = "";

                        String getterName;
                        Method getterMethod;
                        Object value;

                        for (String s : fields_to_concat) {
                            getterName = "get" + s.substring(0,1).toUpperCase() + s.substring(1);
                            // Obtenir la méthode getter
                            getterMethod = dto.getClass().getMethod(getterName);
                            // Invoker la méthode getter
                            value = getterMethod.invoke(dto);

                            value_concated = value_concated.concat(" " + value);
                        }

                        cell.setCellValue(!value_concated.isEmpty() ? value_concated:"");
                    } else if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.DATE)) {

                        Cell cell = page.getRow(configDto.getL_number()).getCell(configDto.getC_number());
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);

                        Method getterMethod = dto.getClass().getMethod(getterName);
                        Object val = getterMethod.invoke(dto);

                        if (val!=null)
                            cell.setCellValue((Date) val);
                    }else{
        //                charger la cellule
                        Cell cell = page.getRow(configDto.getL_number()).getCell(configDto.getC_number());
                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);
                        // Obtenir la méthode getter
                        Method getterMethod = dto.getClass().getMethod(getterName);

                        // Invoker la méthode getter
                        Object value = getterMethod.invoke(dto);
                        if (value!=null)

                            // Insérer/modifier la valeur de la cellule
                            cell.setCellValue((String) value);
                        }
                }

            }


            for (ConfigModelExcelDto configDtoE: configEntreprise){
                Sheet page = workbook.getSheet(configDtoE.getPage());
//                charger la cellule
                if (page!=null){
                    configDtoE.setL_number(configDtoE.getL_number()-1);
                    configDtoE.setC_number(configDtoE.getC_number()-1);
                    if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.BOOLEAN)){
                        Cell cell = page.getRow(configDtoE.getL_number()).getCell(configDtoE.getC_number());
                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                        // Obtenir la méthode getter
//                        entrepriseDto.getI();

                        Method getterMethod = entrepriseDto.getClass().getMethod(getterName);

                        // Invoker la méthode getter
                        Object val = getterMethod.invoke(entrepriseDto);

                        if (val!=null){
                            cell.setCellValue((Boolean) val ? "X" : "");
                        }
                        continue;
                    }  else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.DATE)) {

                        Cell cell = page.getRow(configDtoE.getL_number()).getCell(configDtoE.getC_number());
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);

                        Method getterMethod = dto.getClass().getMethod(getterName);
                        Object val = getterMethod.invoke(dto);

                        if (val!=null)
                            cell.setCellValue((Date) val);
                    } if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.NUMBER)) {

                        Cell cell = page.getRow(configDtoE.getL_number()).getCell(configDtoE.getC_number());
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);

                        Method getterMethod = dto.getClass().getMethod(getterName);
                        Object val = getterMethod.invoke(dto);
                        if (val!=null)
                            cell.setCellValue((Integer) val);

                    }else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.LIST)) {
                        List<String> fields_entity = Arrays.stream(configDtoE.getClass_fields_if_list().split(",")).toList();
                        if (entrepriseDto.getId_()!=null && entrepriseDto.getId_()!=0) {
                            if (configDtoE.getField().toLowerCase().contains("banque")){
                                List<BanqueEntrepriseDto> banques = banqueEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  banques.size(); j++){
                                    var banque=banques.get(j);
                                    var col_=0;
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);
//                                        var code = getExcelCodeLetter(configDtoE.getC_number()+col_) + (configDtoE.getL_number()+j);
                                        Cell cell = page.getRow(configDtoE.getL_number()+j).getCell(configDtoE.getC_number()+col_);
                                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                                        String getterName = "get" + field_entity.substring(0,1).toUpperCase() + field_entity.substring(1);
                                        // Obtenir la méthode getter
                                        Method getterMethod = banque.getClass().getMethod(getterName);
                                        // Invoker la méthode getter
                                        Object value = getterMethod.invoke(banque);

                                        cell.setCellValue(value != null ? (String) value : "");
                                        col_+=10;
                                    }
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("dirigent")) {
                                List<DirigeantEntrepriseDto> dirigeants = dirigeantEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  dirigeants.size(); j++){
                                    var dirigeant=dirigeants.get(j);
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);
//                                        String letter = getExcelCodeLetter(configDtoE.getC_number()+i);
                                        var ligne = configDtoE.getL_number()+j;

                                        Cell cell = page.getRow(configDtoE.getL_number()+j).getCell(configDtoE.getC_number()+i);
                                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                                        String getterName = "get" + field_entity.substring(0,1).toUpperCase() + field_entity.substring(1);
                                        // Obtenir la méthode getter
                                        Method getterMethod = dirigeant.getClass().getMethod(getterName);
                                        // Invoker la méthode getter
                                        Object value = getterMethod.invoke(dirigeant);

                                        cell.setCellValue(value!=null ? (String) value : "");
                                    }
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("gerant")) {
                                List<GerantEntrepriseDto> gerants = gerantEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  gerants.size(); j++){
                                    var gerant=gerants.get(j);
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);

                                        Cell cell = page.getRow(configDtoE.getL_number()+j).getCell(configDtoE.getC_number()+i);

                                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                                        String getterName = "get" + field_entity.substring(0,1).toUpperCase() + field_entity.substring(1);
                                        // Obtenir la méthode getter
                                        Method getterMethod = gerant.getClass().getMethod(getterName);
                                        // Invoker la méthode getter
                                        Object value = getterMethod.invoke(gerant);

                                        cell.setCellValue(value!=null ? (String) value:"");
                                    }
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("activite")) {
                                List<ActiviteEntrepriseDto> activites = activiteEntrepriseService.getListByEntreprise(entrepriseDto.getId_());

                                Cell cell = null;
                                var rw = 0;
                                for (int j=0; j <  activites.size(); j++){
                                    var activite=activites.get(j);
                                    var ligne = configDtoE.getL_number()+rw;
//                                designation activites

                                    cell = page.getRow(ligne).getCell(configDtoE.getC_number());

                                    cell.setCellValue(activite.getName()!=null ? activite.getName():"");
                                    //                              nomenclature
                                    if (activite.getNomanclature()>0)
                                        for (int i = 0; i < String.valueOf(activite.getNomanclature()).toCharArray().length; i++) {
                                            var num = String.valueOf(activite.getNomanclature()).toCharArray()[i];
//                                            String letter_nomanclature = getExcelCodeLetter (configDtoE.getC_number()+2+i);

                                            cell = page.getRow(ligne).getCell(configDtoE.getC_number()+2+i);

                                            cell.setCellValue(Integer.parseInt(num+""));
                                        }
//                                formule CA
                                    cell = page.getRow(ligne).getCell(configDtoE.getC_number()+10);
                                    cell.setCellValue(activite.getFormuleCa() != null ? activite.getFormuleCa() : "");

                                    rw+=3;
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("actionaire")) {
                                List<ActionaireEntrepriseDto> actionaires = actionaireEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  actionaires.size(); j++){
                                    var actionaire=actionaires.get(j);
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);

                                        var ligne = configDtoE.getL_number()+j;

                                        Cell cell = page.getRow(ligne).getCell(configDtoE.getC_number()+i);

                                        if (field_entity.toLowerCase().contains("name") || field_entity.toLowerCase().contains("surname")) {
                                            cell.setCellValue(actionaire.getName() + " " + actionaire.getSurname());
                                        } else if (field_entity.toLowerCase().contains("empty")){

                                        }else {
                                            String getterName = "get" + field_entity.substring(0, 1).toUpperCase() + field_entity.substring(1);
                                            // Obtenir la méthode getter
                                            Method getterMethod = actionaire.getClass().getMethod(getterName);
                                            // Invoker la méthode getter
                                            Object value = getterMethod.invoke(actionaire);

                                            if (field_entity.toLowerCase().contains("montant")){
                                                cell = page.getRow(ligne).getCell(configDtoE.getC_number()+1+i);
                                                cell.setCellValue((Double) value);
                                            }else{
                                                cell.setCellValue(value != null ? (String) value:"");
                                            }
                                        }
                                    }
                                }

                            }
                        }

                    } else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.FUSE)) {
                        Cell cell = page.getRow(configDtoE.getL_number()).getCell(configDtoE.getC_number());

                        List<String> fields_to_concat = Arrays.stream(configDtoE.getClass_fields_if_list().split(",")).toList();
                        String value_concated = "";

                        String getterName;
                        Method getterMethod;
                        Object value;

                        for (String s : fields_to_concat) {
                            getterName = "get" + s.substring(0,1).toUpperCase() + s.substring(1);
                            // Obtenir la méthode getter
                            getterMethod = entrepriseDto.getClass().getMethod(getterName);
                            // Invoker la méthode getter
                            value = getterMethod.invoke(entrepriseDto);

                            value_concated = value_concated.concat(" " + value);
                        }

                        cell.setCellValue(value_concated);

                    }else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.LISTNUMBER)) {
                        Cell cell = null;
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                        Method getterMethod = entrepriseDto.getClass().getMethod(getterName);

                        var value = getterMethod.invoke(entrepriseDto);
                        if (value!=null){
                            var h = String.valueOf(value).toCharArray();

                            for (int i = 0; i < h.length; i++) {
                                var num = h[i];
                                cell = page.getRow(configDtoE.getL_number()).getCell(configDtoE.getC_number()+i);

                                cell.setCellValue(Integer.parseInt(num+""));
                            }
                        }
                    } else{
                        Cell cell = page.getRow(configDtoE.getL_number()).getCell(configDtoE.getC_number());

                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                        // Obtenir la méthode getter
                        Method getterMethod = entrepriseDto.getClass().getMethod(getterName);
                        // Invoker la méthode getter
                        Object value = getterMethod.invoke(entrepriseDto);

                        cell.setCellValue(value != null ? (String) value:"");
                    }

                }

            }
//
            workbook.setForceFormulaRecalculation(true);
//

            FileOutputStream fileOut = new FileOutputStream(dto.getExcelPath());
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            return dto;

    }

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

    public void complement(boolean sage, String etatFinNpath, String etatFinN_1Path) throws Exception {
        var matrices = jsonService.getAllMatrice(sage);

        var etatNWorkbook = new XSSFWorkbook(new FileInputStream(etatFinNpath));
        var etatN_1Workbook = new XSSFWorkbook(new FileInputStream(etatFinN_1Path));


        for (JsonModel matrice:matrices){
            Sheet pageN = etatNWorkbook.getSheet(matrice.getPageN());
            Sheet pageN_1 = etatN_1Workbook.getSheet(matrice.getPageN_1());

            for (JsonMatriceModel model: matrice.getMatrice()) {

                var rowN = pageN.getRow(model.getRowN());
                var rowN_1 = pageN_1.getRow(model.getRowN_1());

                Cell cellN = rowN.getCell(model.getColN());
                Cell cellN_1 = rowN_1.getCell(model.getColN_1());

                if (cellN_1 != null) {
                    copyCellValue(cellN_1, cellN);
                }

            }

        }

        FileOutputStream fos = new FileOutputStream("public/dossier/test2.xlsx");
        etatNWorkbook.write(fos);
        fos.close();
        etatNWorkbook.close();
        etatN_1Workbook.close();
    }

    public void complement(boolean sage, String etatFinNpath, String etatFinN_1Path, String password) throws Exception {
        var matrices = jsonService.getAllMatrice(sage);
        var etatNWorkbook = new XSSFWorkbook(new FileInputStream(etatFinNpath));
        var etatN_1Workbook = new XSSFWorkbook(new FileInputStream(etatFinN_1Path));


        for (JsonModel matrice:matrices){
            Sheet pageN = etatNWorkbook.getSheet(matrice.getPageN());
            Sheet pageN_1 = etatN_1Workbook.getSheet(matrice.getPageN_1());

            for (JsonMatriceModel model: matrice.getMatrice()) {

                var rowN = pageN.getRow(model.getRowN());
                var rowN_1 = pageN_1.getRow(model.getRowN_1());

                if (rowN != null) {

                    Cell cellN = rowN.getCell(model.getColN());
                    Cell cellN_1 = rowN_1.getCell(model.getColN_1());

                    if (cellN != null) {
                        copyCellValue(cellN_1, cellN);
                    }
                }
            }

        }


        FileOutputStream fos = new FileOutputStream(etatFinNpath);
        etatNWorkbook.write(fos);
        fos.close();
        etatNWorkbook.close();
        etatN_1Workbook.close();
    }

    private void copyCellValue(Cell source, Cell dest) {
        if (source == null) {
            dest.setBlank();
            return;
        }

        switch (source.getCellType()) {
            case STRING:
                dest.setCellValue(source.getStringCellValue());
                break;
            case NUMERIC:
                dest.setCellValue(source.getNumericCellValue());
                break;
            case BOOLEAN:
                dest.setCellValue(source.getBooleanCellValue());
                break;
            case FORMULA:
                dest.setCellFormula(source.getCellFormula());
                break;
            case BLANK:
                break;
            default:
                dest.setCellValue(source.toString());
        }
    }
}
