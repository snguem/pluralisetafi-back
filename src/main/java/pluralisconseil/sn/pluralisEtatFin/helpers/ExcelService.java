package pluralisconseil.sn.pluralisEtatFin.helpers;

import com.aspose.cells.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.models.*;
import pluralisconseil.sn.pluralisEtatFin.data.entities.BanqueEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.GerantEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeAttributIsConfig;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;
import pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.*;

//import java.io.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

@RequiredArgsConstructor
@Service
public class ExcelService {
    private final HelperService helperService;
    private final ConfigModelExcelService configModelExcelService;
    private final ActionaireEntrepriseService actionaireEntrepriseService;
    private final ActiviteEntrepriseService activiteEntrepriseService;
    private final BanqueEntrepriseService banqueEntrepriseService;
    private final DirigeantEntrepriseService dirigeantEntrepriseService;
    private final GerantEntrepriseService gerantEntrepriseService;

    public byte[] toPdf(String excelPath) {
        try {
            Workbook workbook = new Workbook(new File(excelPath).getPath());

            // Parcourir toutes les feuilles du classeur
            for (int i = 0; i < workbook.getWorksheets().getCount(); i++) {
                Worksheet sheet = workbook.getWorksheets().get(i);
                PageSetup pageSetup = sheet.getPageSetup();

                // 📄 Définir format papier A4
                pageSetup.setPaperSize(PaperSizeType.PAPER_A_4);

                // Orientation : Portrait | Landscape
//                pageSetup.setOrientation(PageOrientationType.PORTRAIT);

                // Marges
                pageSetup.setLeftMargin(1.5);
                pageSetup.setRightMargin(1);
                pageSetup.setTopMargin(1.5);
                pageSetup.setBottomMargin(1.5);

                // ALignement horizontal et vertical
                pageSetup.setCenterHorizontally(true);
                pageSetup.setCenterVertically(false);

                // adaptation format A4
                pageSetup.setFitToPagesWide(1);
                pageSetup.setFitToPagesTall(1);
            }

            // Convertir en PDF
            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            workbook.save(pdfStream, SaveFormat.PDF);

            // Retourner le PDF
            return pdfStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

   
    public byte[] toPdf(String excelPath, List<String> pages) {
        try {
            Workbook originalWorkbook = new Workbook(excelPath);

            // Nouveau classeur vide
            Workbook newWorkbook = new Workbook();
            // Supprimer la feuille vide par défaut
            newWorkbook.getWorksheets().clear();

            // Copier uniquement les feuilles de la liste `pages`
            for (String sheetName : pages) {
                Worksheet sheet = originalWorkbook.getWorksheets().get(sheetName);
                if (sheet != null) {
                    int idx = newWorkbook.getWorksheets().add();
                    Worksheet copiedSheet = newWorkbook.getWorksheets().get(idx);
                    copiedSheet.copy(sheet);
                    copiedSheet.setName(sheet.getName());

                    // Mise en page pour occuper toute la feuille PDF
                    PageSetup pageSetup = copiedSheet.getPageSetup();

                    pageSetup.setPaperSize(PaperSizeType.PAPER_A_4);

                    // Marges minimales
                    pageSetup.setLeftMargin(1.5);
                    pageSetup.setRightMargin(1);
                    pageSetup.setTopMargin(1.5);
                    pageSetup.setBottomMargin(1.5);

                    // Optimisation affichage : scalé sur la feuille entière
                    pageSetup.setFitToPagesWide(1);
                    pageSetup.setFitToPagesTall(1);

                    // Optionnel : forcer le zoom à 100% si contenu déjà petit
                    // pageSetup.setZoom(100);

                    // (Optionnel) Détection et orientation automatique
                    // pageSetup.setOrientation(PageOrientationType.LANDSCAPE);
                }
            }

            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            newWorkbook.save(pdfStream, SaveFormat.PDF);


            // Enregistrer temporairement ou retourner selon besoin
            String pathName = helperService.saveTempFile(pdfStream.toByteArray());
            byte[] pdfBytes = maskMarkDefault(pathName);
            helperService.remove(pathName);
            return pdfBytes;
        } catch (Exception e) {
            return null;
        }
    }

   
    public byte[] maskMark(String pdfFile, float x, float y, float height, float width) throws IOException {

        // Charger le PDF depuis le MultipartFile
        File file = new File(pdfFile);
        InputStream inputStream = new FileInputStream(file);

        PDDocument doc = PDDocument.load(inputStream);

        // Parcourir toutes les pages pour recouvrir le texte
        for (PDPage page : doc.getPages()) {

            PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

            // Choisir la couleur de recouvrement (blanc ici)
            contentStream.setNonStrokingColor(255, 255, 255);

            // Dessiner le rectangle pour recouvrir le texte
            contentStream.addRect(x, y, width, height);
            contentStream.fill();

            contentStream.close();
        }

        // Sauvegarder le PDF modifié dans un tableau de bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        // Retourner le PDF modifié
        return baos.toByteArray();
    }

    
    public byte[] maskMarkDefault(String pdfFile) throws IOException {

        // Charger le PDF depuis le MultipartFile
        File file = new File(pdfFile);
        InputStream inputStream = new FileInputStream(file);

        PDDocument doc = PDDocument.load(inputStream);

        // Parcourir toutes les pages pour recouvrir le texte
        for (PDPage page : doc.getPages()) {

            // Coordonnées et taille approximatives du texte à recouvrir
            float x = 0;   // position X
            float y = 0;   // position Y
            float width = 500;  // largeur du rectangle
            float height = 20;  // hauteur du rectangle

            PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

            // Choisir la couleur de recouvrement (blanc ici)
            contentStream.setNonStrokingColor(255, 255, 255);

            // Dessiner le rectangle pour recouvrir le texte
            contentStream.addRect(x, y, width, height);
            contentStream.fill();

            contentStream.close();
        }

        // Sauvegarder le PDF modifié dans un tableau de bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        // Retourner le PDF modifié
        return baos.toByteArray();
    }


    public EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path) throws Exception {

            if (!helperService.isExist(model_path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier du model est introuvable");
//            if (helperService.isExist(balance_n_path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("La balanace N est introuvable");
            // chargement de la balance n
            Workbook balance_n = new Workbook(new File(balance_n_path).getPath());
            // chargement du model
            Workbook workbook = new Workbook(new File(model_path).getPath());

            // Accéder à la première feuille
            Worksheet balN = workbook.getWorksheets().get("BAL N");

//            integration de la balence N au modele
            // Accéder aux cellules
            Cells cells_baln_model = balN.getCells();
            Cells cells_baln = balance_n.getWorksheets().get(0).getCells();

            // Parcourir les lignes (par exemple les 10 premières)
            for (int row = 1; row < 200; row++) {
                // Accéder à la cellule de la colonne 0 (colonne A)
                for (int col = 0; col < 8; col++) {
                    Cell cell = cells_baln_model.get(row, col);
                    // Insérer/modifier la valeur de la cellule
                    cell.setValue(cells_baln.get(row, col).getValue());
                }
            }

//          path excel
            String pathExcel = model_path.replace("modele_", dto.getId()+"_etat_financier_"+dto.getEntreprise().getNameEntreprise()+dto.getAnnee_n()+"_");
            pathExcel = pathExcel.replace(" ", "_");
            dto.setExcelPath(pathExcel);
//          forcer le recalcul des formules
            workbook.calculateFormula();
            // Enregistrer le fichier modifié
            workbook.save(pathExcel);
            return dto;

    }

    public EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path, String balance_n_1_path) throws Exception {

        if (!helperService.isExist(model_path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier du model est introuvable");
//        if (helperService.isExist(balance_n_path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("La balanace N est introuvable");
//        if (helperService.isExist(balance_n_1_path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("La balanace N-1 est introuvable");
        // chargement de la balance n
        Workbook balance_n = new Workbook(new File(balance_n_path).getPath());
        // chargement de la balance n-1
        Workbook balance_n_1 = new Workbook(new File(balance_n_1_path).getPath());
        // chargement du model
        Workbook workbook = new Workbook(new File(model_path).getPath());

        // Accéder à la première feuille
        Worksheet balN = workbook.getWorksheets().get("BAL N");
        Worksheet balN_1 = workbook.getWorksheets().get("BAL N-1");


//            integration de la balence N au modele
        // Accéder aux cellules
        Cells cells_baln_model = balN.getCells();
        Cells cells_baln_1_model = balN_1.getCells();
        Cells cells_baln = balance_n.getWorksheets().get(0).getCells();
        Cells cells_baln_1 = balance_n_1.getWorksheets().get(0).getCells();

        // Parcourir les lignes (par exemple les 10 premières)
        for (int row = 1; row < 200; row++) {
            // Accéder à la cellule de la colonne 0 (colonne A)
            for (int col = 0; col < 8; col++) {
                Cell cell = cells_baln_model.get(row, col);
                Cell cell_1 = cells_baln_1_model.get(row, col);
                // Insérer/modifier la valeur de la cellule
                cell.setValue(cells_baln.get(row, col).getValue());
                cell_1.setValue(cells_baln_1.get(row, col).getValue());
            }
        }

//          path excel
        String pathExcel = model_path.replace("modele_", dto.getId()+"etat_financier_"+dto.getEntreprise().getNameEntreprise()+dto.getAnnee_n()+"_");
        pathExcel = pathExcel.replace(" ", "_");
        dto.setExcelPath(pathExcel);
//          forcer le recalcul des formules
        workbook.calculateFormula();
        // Enregistrer le fichier modifié
        workbook.save(pathExcel);
        return dto;

    }


    public List<String> getExcelPageNames(String path) throws Exception {
        List<String> names=new ArrayList<>();
        if (!helperService.isExist(path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier est introuvable");

        Workbook excel_path = new Workbook(new File(path).getPath());

        for (int i = 0; i < excel_path.getWorksheets().getCount(); i++) {
            Worksheet sheet = excel_path.getWorksheets().get(i);
            if (!sheet.getName().equalsIgnoreCase("bal n") && !sheet.getName().equalsIgnoreCase("bal n-1"))
                names.add(sheet.getName());
        }

        return names;
    }


    public void setPartModelExcel(String path, int part) throws Exception {
        if (!helperService.isExist(path)) throw new FileNotFoundException("Le fichier est introuvable");

        Workbook workbook = new Workbook(new File(path).getPath());
        Worksheet page = workbook.getWorksheets().get("Note 13");
        if (page!=null){
            Cell cell = page.getCells().get("G7");
            cell.setValue(part);
            workbook.save(path);
        }

    }
    
    public EtatFinancierDto configExcelFile(EtatFinancierDto dto, EntrepriseSubstituteDto entrepriseDto) throws Exception{
            if (!helperService.isExist(dto.getExcelPath())) throw new FileNotFoundException("Le fichier est introuvable");

            // chargement du model
            Workbook workbook = new Workbook(new File(dto.getExcelPath()).getPath());
//            chargement des configs
//            List<Map<String, Object>> configsModel1 = SetModelPageValues.model1ConfigEntreprise();
            Map<String, String> filter=new HashMap<>();
            filter.put("type_config", TypeConfig.ETATFINANCIER.name());
            List<ConfigModelExcelDto> configEtats = configModelExcelService.getAll(filter);
            filter.replace("type_config", TypeConfig.ENTREPRISE.name());
            List<ConfigModelExcelDto> configEntreprise = configModelExcelService.getAll(filter);

            for (ConfigModelExcelDto configDto: configEtats){
                Worksheet page = workbook.getWorksheets().get(configDto.getPage());
                if (page!=null){
                    if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.BOOLEAN)) {

                        Cell cell = page.getCells().get(configDto.getCodeExcel());
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);

                        Method getterMethod = dto.getClass().getMethod(getterName);
                        Object val = getterMethod.invoke(dto);
                        boolean value =val!=null ? (Boolean) val:false;

                        cell.setValue(value ? "X" : "");
                    }else if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.LISTNUMBER)) {
                        Cell cell = null;
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);
                        Method getterMethod = dto.getClass().getMethod(getterName);
                        for (int i = 0; i < String.valueOf(getterMethod.invoke(dto)).toCharArray().length; i++) {
                            var num = String.valueOf(getterMethod.invoke(dto)).toCharArray()[i];
                            String letter = getExcelCodeLetter(configDto.getC_number()+i);
                            cell = page.getCells().get(letter + configDto.getL_number());
                            cell.setValue(num);
                        }
                    } else if (configDto.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.FUSE)) {
                        Cell cell = page.getCells().get(configDto.getCodeExcel());
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

                        cell.setValue(value_concated);
                    }else{
        //                charger la cellule
                        Cell cell = page.getCells().get(configDto.getCodeExcel());
                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                        String getterName = "get" + configDto.getField().substring(0,1).toUpperCase() + configDto.getField().substring(1);
                        // Obtenir la méthode getter
                        Method getterMethod = dto.getClass().getMethod(getterName);

                        // Invoker la méthode getter
                        Object value = getterMethod.invoke(dto);
                        if (value!=null)
                            // Insérer/modifier la valeur de la cellule
                            cell.setValue(value);
                        }
                }

            }


            for (ConfigModelExcelDto configDtoE: configEntreprise){
                Worksheet page = workbook.getWorksheets().get(configDtoE.getPage());
//                charger la cellule
                if (page!=null){
                    if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.BOOLEAN)){
                        Cell cell = page.getCells().get(configDtoE.getCodeExcel());
                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                        // Obtenir la méthode getter
//                        entrepriseDto.getI();

                        Method getterMethod = entrepriseDto.getClass().getMethod(getterName);
                        // Invoker la méthode getter
                        Object val = getterMethod.invoke(entrepriseDto);
                        boolean value =val!=null ? (Boolean) val:false;

                        cell.setValue(value ? "X" : "");
                    } else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.LIST)) {
                        List<String> fields_entity = Arrays.stream(configDtoE.getClass_fields_if_list().split(",")).toList();
                        if (entrepriseDto.getId_()!=null && entrepriseDto.getId_()!=0) {
                            if (configDtoE.getField().toLowerCase().contains("banque")){
                                List<BanqueEntrepriseDto> banques = banqueEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  banques.size(); j++){
                                    var banque=banques.get(j);
                                    var col_=0;
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);
                                        var code = getExcelCodeLetter(configDtoE.getC_number()+col_) + (configDtoE.getL_number()+j);
                                        Cell cell = page.getCells().get(code);
                                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                                        String getterName = "get" + field_entity.substring(0,1).toUpperCase() + field_entity.substring(1);
                                        // Obtenir la méthode getter
                                        Method getterMethod = banque.getClass().getMethod(getterName);
                                        // Invoker la méthode getter
                                        Object value = getterMethod.invoke(banque);

                                        cell.setValue(value);
                                        col_+=10;
                                    }
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("dirigent")) {
                                List<DirigeantEntrepriseDto> dirigeants = dirigeantEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  dirigeants.size(); j++){
                                    var dirigeant=dirigeants.get(j);
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);
                                        String letter = getExcelCodeLetter(configDtoE.getC_number()+i);
                                        var ligne = configDtoE.getL_number()+j;

                                        Cell cell = page.getCells().get(letter + ligne);
                                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                                        String getterName = "get" + field_entity.substring(0,1).toUpperCase() + field_entity.substring(1);
                                        // Obtenir la méthode getter
                                        Method getterMethod = dirigeant.getClass().getMethod(getterName);
                                        // Invoker la méthode getter
                                        Object value = getterMethod.invoke(dirigeant);

                                        cell.setValue(value);
                                    }
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("gerant")) {
                                List<GerantEntrepriseDto> gerants = gerantEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  gerants.size(); j++){
                                    var gerant=gerants.get(j);
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);
                                        String letter = getExcelCodeLetter (configDtoE.getC_number()+i);
                                        var ligne = configDtoE.getL_number()+j;

                                        Cell cell = page.getCells().get(letter + ligne);
                                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                                        String getterName = "get" + field_entity.substring(0,1).toUpperCase() + field_entity.substring(1);
                                        // Obtenir la méthode getter
                                        Method getterMethod = gerant.getClass().getMethod(getterName);
                                        // Invoker la méthode getter
                                        Object value = getterMethod.invoke(gerant);

                                        cell.setValue(value);
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
                                    String letter_designation = getExcelCodeLetter (configDtoE.getC_number());
                                    cell = page.getCells().get(letter_designation + ligne);
                                    cell.setValue(activite.getName());
                                    //                              nomenclature
                                    for (int i = 0; i < String.valueOf(activite.getNomanclature()).toCharArray().length; i++) {
                                        var num = String.valueOf(activite.getNomanclature()).toCharArray()[i];
                                        String letter_nomanclature = getExcelCodeLetter (configDtoE.getC_number()+2+i);
                                        cell = page.getCells().get(letter_nomanclature + ligne);
                                        cell.setValue(num);
                                    }
//                                formule CA
                                    String letter_form_ca = getExcelCodeLetter (configDtoE.getC_number()+10);
                                    cell = page.getCells().get(letter_form_ca + ligne);
                                    if (activite.getFormuleCa().contains("="))
                                        cell.setFormula(activite.getFormuleCa());
                                    else
                                        cell.setValue(activite.getFormuleCa());

                                    rw+=3;
                                }
                            } else if (configDtoE.getField().toLowerCase().contains("actionaire")) {
                                List<ActionaireEntrepriseDto> actionaires = actionaireEntrepriseService.getListByEntreprise(entrepriseDto.getId_());
                                for (int j=0; j <  actionaires.size(); j++){
                                    var actionaire=actionaires.get(j);
                                    for (int i = 0; i < fields_entity.size(); i++) {
                                        var field_entity=fields_entity.get(i);

                                        String letter = getExcelCodeLetter ( configDtoE.getC_number()+i);
                                        var ligne = configDtoE.getL_number()+j;

                                        Cell cell = page.getCells().get(letter + ligne);
                                        if (field_entity.toLowerCase().contains("name") || field_entity.toLowerCase().contains("surname")) {
                                            cell.setValue(actionaire.getName() + " " + actionaire.getSurname());
                                        } else if (field_entity.toLowerCase().contains("empty")){
//                                            cell.setValue("Here is test");
                                        }else {
                                            String getterName = "get" + field_entity.substring(0, 1).toUpperCase() + field_entity.substring(1);
                                            // Obtenir la méthode getter
                                            Method getterMethod = actionaire.getClass().getMethod(getterName);
                                            // Invoker la méthode getter
                                            Object value = getterMethod.invoke(actionaire);

                                            if (field_entity.toLowerCase().contains("montant")){
                                                cell = page.getCells().get(getExcelCodeLetter ( configDtoE.getC_number()+i+1) + ligne);
                                            }
                                            cell.setValue(value);
                                        }
                                    }
                                }

                            }
                        }

                    } else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.FUSE)) {
                        Cell cell = page.getCells().get(configDtoE.getCodeExcel());
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

                        cell.setValue(value_concated);

                    }else if (configDtoE.getTypeAttributIsConfigConfig().equals(TypeAttributIsConfig.LISTNUMBER)) {
                        Cell cell = null;
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                        Method getterMethod = entrepriseDto.getClass().getMethod(getterName);

                        var h = String.valueOf(getterMethod.invoke(entrepriseDto)).toCharArray();

                        for (int i = 0; i < h.length; i++) {
                            var num = h[i];
                            String letter = getExcelCodeLetter ( configDtoE.getC_number()+i);
                            cell = page.getCells().get(letter +  configDtoE.getL_number());
                            cell.setValue(num);
                        }
                    } else{
                        Cell cell = page.getCells().get(configDtoE.getCodeExcel());
                        // Construire le nom du getter : get + nom de l'attribut avec majuscule
                        String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                        // Obtenir la méthode getter
                        Method getterMethod = entrepriseDto.getClass().getMethod(getterName);
                        // Invoker la méthode getter
                        Object value = getterMethod.invoke(entrepriseDto);

                        cell.setValue(value);
                    }

                }

            }

//          forcer le recalcul des formules
            workbook.calculateFormula();
            // Enregistrer le fichier modifié
            workbook.save(dto.getExcelPath());
            return dto;

    }

    private String getExcelCodeLetter(int n) {
        var first_letter = n>26? (char) ('A' + (n/26)-1): "";
        var second_letter = (char) ('A' + ((n%26)-1));

        return "" +first_letter + second_letter;
    }

}
