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
public class AposePdfExcelService implements ExcelFormulaCalculator {
    private final HelperService helperService;

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
