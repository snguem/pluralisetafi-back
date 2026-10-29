package pluralisconseil.sn.pluralisEtatFin.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelPdfPageService;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobConverterExcelPdflService implements ExcelToPdfConverter {
    private final DocumentConverter converter;
    private final ModelPdfPageService modelPdfPageService;

    @Override
    public PdfExport export(String excelPath, List<String> pages, Long modelId) {
        try {
            int sheetCount = 0, fromModel = 0;
            pages = pages.stream().map(String::toLowerCase).toList();
            // what the model sets for its sheets; the others keep the default rules
            var modelSettings = modelPdfPageService.settingsByModel(modelId);

            InputStream is = new FileInputStream(excelPath);
            Workbook workbook = new XSSFWorkbook(is);

//            suppression des pages qui ne sont pas dans la liste
            for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
                Sheet currentSheet = workbook.getSheetAt(i);
                String sheetName = currentSheet.getSheetName().toLowerCase();

                if (!pages.contains(sheetName)) {
                    workbook.removeSheetAt(i);
                }else{
//                    currentSheet.setFitToPage(true);
                    PrintSetup ps = currentSheet.getPrintSetup();

                    ps.setFitWidth((short) 1);
                    ps.setFitHeight((short) 1);
                    ps.setPaperSize(PrintSetup.A4_PAPERSIZE);

                    currentSheet.setMargin(PageMargin.LEFT, 0.1);
                    currentSheet.setMargin(PageMargin.RIGHT, 0.1);
                    currentSheet.setMargin(PageMargin.TOP, 0.1);
                    currentSheet.setMargin(PageMargin.BOTTOM, 0.1);
                    currentSheet.setHorizontallyCenter(true);
                    currentSheet.setVerticallyCenter(false);
                    ps.setLandscape(false);

                    PdfPageSettings settings = modelSettings.get(sheetName);
                    sheetCount++;
                    if (settings != null) fromModel++;
                    else settings = LegacyPdfPageRules.forSheet(sheetName);
                    if (settings.printArea() != null)
                        workbook.setPrintArea(i, settings.printArea());
                    ps.setLandscape(settings.landscape());
                }
            }

            File tempExcel = File.createTempFile("etat_fin", ".xlsx");
            File tempExcelOut = File.createTempFile("etat_fin_pdf_out", ".pdf");
//           Avant la conversion PDF
            workbook.setForceFormulaRecalculation(true);

            try (FileOutputStream fos = new FileOutputStream(tempExcel)) {
                workbook.write(fos);
            }
            workbook.close();

            File inputFile = new File(tempExcel.toString());
            File outputFile = new File(tempExcelOut.toString());

            converter
                    .convert(inputFile)
                    .as(DefaultDocumentFormatRegistry.XLSX)
                    .to(outputFile)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();

            // Retourner le PDF
            byte[] pdfBytes = Files.readAllBytes(tempExcelOut.toPath());

            tempExcel.delete();
            tempExcelOut.delete();

            log.info("PDF de {}: {} feuille(s), {} avec les réglages du modèle {} ({} réglage(s) configuré(s))",
                    excelPath, sheetCount, fromModel, modelId, modelSettings.size());
            return new PdfExport(pdfBytes, sheetCount, fromModel);
        } catch (Exception e) {
            log.error("Echec de la conversion en PDF de {}", excelPath, e);
            return null;
        }
    }


}
