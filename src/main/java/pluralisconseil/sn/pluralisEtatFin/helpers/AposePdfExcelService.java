package pluralisconseil.sn.pluralisEtatFin.helpers;

import com.aspose.cells.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

@RequiredArgsConstructor
@Service
public class AposePdfExcelService {
    private final HelperService helperService;

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
                pageSetup.setLeftMargin(0.2);
                pageSetup.setRightMargin(0.1);
                pageSetup.setTopMargin(0.2);
                pageSetup.setBottomMargin(0.2);

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

                    int maxRow = copiedSheet.getCells().getMaxDataRow();
                    int maxCol = copiedSheet.getCells().getMaxDataColumn();

                    // Aspose a un helper pour convertir les index (ex: 0,0) en nom (ex: "A1")
                    // On construit la zone: "A1:Z50" par exemple
                    String lastCellName = CellsHelper.cellIndexToName(maxRow, maxCol);

                    // 📄 Définir format papier A4
                    pageSetup.setPaperSize(PaperSizeType.PAPER_A_4);

                    // Orientation : Portrait | Landscape
//                pageSetup.setOrientation(PageOrientationType.PORTRAIT);

                    // Marges
                    pageSetup.setLeftMargin(0.2);
                    pageSetup.setRightMargin(0.2);
                    pageSetup.setTopMargin(0.3);
                    pageSetup.setBottomMargin(0.2);

                    // ALignement horizontal et vertical
                    pageSetup.setCenterHorizontally(true);
                    pageSetup.setCenterVertically(false);

                    // adaptation format A4
                    pageSetup.setFitToPagesWide(1);
                    pageSetup.setFitToPagesTall(1);

                    if (sheetName.toLowerCase().contains("page de garde"))
                        lastCellName = "AM76";
                    else if (sheetName.toLowerCase().contains("r1"))
                        lastCellName = "AN84";
                    else if (sheetName.toLowerCase().contains("r2"))
                        lastCellName = "T61";
                    else if (sheetName.toLowerCase().contains("r3"))
                        lastCellName = "F50";
                    else if (sheetName.toLowerCase().contains("r4"))
                        lastCellName = "F54";
                    else if (sheetName.toLowerCase().contains("bilan"))
                        lastCellName = "L36";
                    else if (sheetName.toLowerCase().contains("compte"))
                        lastCellName = "F48";
                    else if (sheetName.toLowerCase().contains("flux"))
                        lastCellName = "F36";
                    else if (sheetName.toLowerCase().contains("note 1"))
                        lastCellName = "F43";
                    else if (sheetName.toLowerCase().contains("note 3a"))
                        lastCellName = "H27";
                    else if (sheetName.toLowerCase().contains("note 3b"))
                        lastCellName = "I20";
                    else if (sheetName.toLowerCase().contains("note 3c"))
                        lastCellName = "E24";
                    else if (sheetName.toLowerCase().contains("note 3d"))
                        lastCellName = "F25";
                    else if (sheetName.toLowerCase().contains("note 4"))
                        lastCellName = "G31";
                    else if (sheetName.toLowerCase().contains("note 5"))
                        lastCellName = "G28";
                    else if (sheetName.toLowerCase().contains("note 6"))
                        lastCellName = "D19";
                    else if (sheetName.toLowerCase().contains("note 7"))
                        lastCellName = "G23";
                    else if (sheetName.toLowerCase().contains("note 8"))
                        lastCellName = "G21";
                    else if (sheetName.toLowerCase().contains("note 9"))
                        lastCellName = "D18";
                    else if (sheetName.toLowerCase().contains("note 10"))
                        lastCellName = "D17";
                    else if (sheetName.toLowerCase().contains("note 11"))
                        lastCellName = "D23";
//                    else if (sheetName.toLowerCase().contains("bal n"))
//                        lastCellName = "M100";


                    // C'est cette ligne qui oblige le PDF à commencer en haut à gauche (A1)
                    pageSetup.setPrintArea("A1:" + lastCellName);
                    // ---------------------------------------------------------

//                    pageSetup.setPaperSize(PaperSizeType.PAPER_A_4);
//
//                    // Marges minimales
//                    pageSetup.setLeftMargin(1);
//                    pageSetup.setRightMargin(1);
//                    pageSetup.setTopMargin(1);
//                    pageSetup.setBottomMargin(1);
//
//                    // Optimisation affichage : scalé sur la feuille entière
//                    pageSetup.setFitToPagesWide(1);
//                    pageSetup.setFitToPagesTall(1);
//
//
//                    pageSetup.setCenterHorizontally(true);
//                    pageSetup.setCenterVertically(false);

                    // Optionnel : forcer le zoom à 100% si contenu déjà petit
//                     pageSetup.setZoom(50);
//                     if (List.of("Note 31", "Note 27A").stream().filter(f -> f.equals(sheetName)).findFirst().orElse(null)!=null)
//                        pageSetup.setZoom(100);

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


    public void calculateFormule(String path) throws Exception {

        if (!helperService.isExist(path)) throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier est introuvable");

        Workbook workbook = new Workbook(new File(path).getPath());

//          forcer le recalcul des formules
        workbook.calculateFormula();

        // Enregistrer le fichier modifié
        workbook.save(path);

    }

}
