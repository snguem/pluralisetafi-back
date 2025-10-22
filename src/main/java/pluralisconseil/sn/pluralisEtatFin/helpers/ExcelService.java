package pluralisconseil.sn.pluralisEtatFin.helpers;

import com.aspose.cells.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ConfigModelExcelService;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ExcelService {
    private final HelperService helperService;
    private final ConfigModelExcelService configModelExcelService;

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
            System.out.println("\n\n"+e.getMessage()+"\n\n");
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


    
    public EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path) {
        try {
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
            String pathExcel = model_path.replace("modele_", dto.getId()+"_etat_financier_"+dto.getEntreprise_name()+dto.getAnnee_n()+"_");
            dto.setExcelPath(pathExcel);
//          forcer le recalcul des formules
            workbook.calculateFormula();
            // Enregistrer le fichier modifié
            workbook.save(pathExcel);
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path, String balance_n_1_path) {
        try {
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
            String pathExcel = model_path.replace("modele_", dto.getId()+"etat_financier_"+dto.getEntreprise_name()+dto.getAnnee_n()+"_");
            dto.setExcelPath(pathExcel);
//          forcer le recalcul des formules
            workbook.calculateFormula();
            // Enregistrer le fichier modifié
            workbook.save(pathExcel);
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    
    public List<String> getExcelPageNames(String path) {
        List<String> names=new ArrayList<>();
        try {
            Workbook excel_path = new Workbook(new File(path).getPath());

            for (int i = 0; i < excel_path.getWorksheets().getCount(); i++) {
                Worksheet sheet = excel_path.getWorksheets().get(i);
                if (!sheet.getName().equalsIgnoreCase("bal n") && !sheet.getName().equalsIgnoreCase("bal n-1"))
                    names.add(sheet.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }

    
    public EtatFinancierDto configExcelFile(EtatFinancierDto dto, EntrepriseDto entrepriseDto) {
        try {
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


            for (ConfigModelExcelDto configDtoE: configEntreprise){
                Worksheet page = workbook.getWorksheets().get(configDtoE.getPage());
//                charger la cellule
                Cell cell = page.getCells().get(configDtoE.getCodeExcel());
                // Construire le nom du getter : get + nom de l'attribut avec majuscule
                String getterName = "get" + configDtoE.getField().substring(0,1).toUpperCase() + configDtoE.getField().substring(1);
                // Obtenir la méthode getter
                Method getterMethod = entrepriseDto.getClass().getMethod(getterName);

                // Invoker la méthode getter
                Object value = getterMethod.invoke(entrepriseDto);
                if (value!=null)
                    // Insérer/modifier la valeur de la cellule
                    cell.setValue(value);
            }

//          forcer le recalcul des formules
            workbook.calculateFormula();
            // Enregistrer le fichier modifié
            workbook.save(dto.getExcelPath());
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
