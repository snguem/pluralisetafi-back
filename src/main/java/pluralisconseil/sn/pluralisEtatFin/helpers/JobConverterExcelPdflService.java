package pluralisconseil.sn.pluralisEtatFin.helpers;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

@RequiredArgsConstructor
@Service
public class JobConverterExcelPdflService {
    private final DocumentConverter converter;

    public byte[] toPdf(String excelPath) {
        try {

            InputStream is = new FileInputStream(excelPath);
            Workbook workbook = new XSSFWorkbook(is);

            File tempExcel = File.createTempFile("etat_fin", ".xlsx");
            File tempExcelOut = File.createTempFile("etat_fin_pdf_out", ".pdf");
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

            return pdfBytes;
        } catch (Exception e) {
            return null;
        }
    }

   
    public byte[] toPdf(String excelPath, List<String> pages) {
        try {
            pages = pages.stream().map(String::toLowerCase).toList();

            InputStream is = new FileInputStream(excelPath);
            Workbook workbook = new XSSFWorkbook(is);

//            suppression des pages qui ne sont pas dans la liste
            for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
                Sheet currentSheet = workbook.getSheetAt(i);
                String sheetName = currentSheet.getSheetName().toLowerCase();

                if (!pages.contains(sheetName)) {
                    workbook.removeSheetAt(i);
                }else{

                    currentSheet.setFitToPage(true);
                    PrintSetup ps = currentSheet.getPrintSetup();
                    currentSheet.setHorizontallyCenter(true);
                    ps.setFitWidth((short) 1);
                    ps.setFitHeight((short) 1);

                    currentSheet.setMargin(PageMargin.LEFT, 0.2);
                    currentSheet.setMargin(PageMargin.RIGHT, 0.2);
                    currentSheet.setMargin(PageMargin.TOP, 0.3);
                    currentSheet.setMargin(PageMargin.BOTTOM, 0.3);

                    if (sheetName.contains("36"))
                        workbook.setPrintArea(i, "$A$1:$H$38");
                    else if (sheetName.contains("35"))
                        workbook.setPrintArea(i, "$A$1:$D$25");
                    else if (sheetName.contains("34"))
                        workbook.setPrintArea(i, "$A$1:$D$63");
                    else if (sheetName.contains("31"))
                        workbook.setPrintArea(i, "$A$1:$F$29");
                    else if (sheetName.contains("32"))
                        workbook.setPrintArea(i, "$A$1:$N$25");
                    else if (sheetName.contains("30"))
                        workbook.setPrintArea(i, "$A$1:$D$29");
                    else if (sheetName.contains("29"))
                        workbook.setPrintArea(i, "$A$1:$D$31");
                    else if (sheetName.contains("28"))
                        workbook.setPrintArea(i, "$A$1:$I$27");
                    else if (sheetName.contains("27b")){
                        workbook.setPrintArea(i, "$A$1:$P$30");
                        ps.setLandscape(true);
                    }
                    else if (sheetName.contains("27a"))
                        workbook.setPrintArea(i, "$A$1:$D$15");
                    else if (sheetName.contains("26"))
                        workbook.setPrintArea(i, "$A$1:$D$17");
                    else if (sheetName.contains("25"))
                        workbook.setPrintArea(i, "$A$1:$D$14");
                    else if (sheetName.contains("24"))
                        workbook.setPrintArea(i, "$A$1:$D$23");
                    else if (sheetName.contains("23"))
                        workbook.setPrintArea(i, "$A$1:$D$13");
                    else if (sheetName.contains("22"))
                        workbook.setPrintArea(i, "$A$1:$D$32");
                    else if (sheetName.contains("21"))
                        workbook.setPrintArea(i, "$A$1:$D$30");
                    else if (sheetName.contains("20"))
                        workbook.setPrintArea(i, "$A$1:$D$19");
                    else if (sheetName.contains("19")){
                        workbook.setPrintArea(i, "$A$1:$H$32");
                        ps.setLandscape(true);
                    }
                    else if (sheetName.contains("18"))
                        workbook.setPrintArea(i, "$A$1:$H$23");
                    else if (sheetName.contains("17"))
                        workbook.setPrintArea(i, "$A$1:$G$18");
                    else if (sheetName.contains("16c"))
                        workbook.setPrintArea(i, "$A$1:$D$15");
                    else if (sheetName.contains("16b bis"))
                        workbook.setPrintArea(i, "$A$1:$E$22");
                    else if (sheetName.contains("16b"))
                        workbook.setPrintArea(i, "$A$1:$F$40");
                    else if (sheetName.contains("16a"))
                        workbook.setPrintArea(i, "$A$1:$H$40");
                    else if (sheetName.contains("15b"))
                        workbook.setPrintArea(i, "$A$1:$G$14");
                    else if (sheetName.contains("15a"))
                        workbook.setPrintArea(i, "$A$1:$H$28");
                    else if (sheetName.contains("14"))
                        workbook.setPrintArea(i, "$A$1:$G$23");
                    else if (sheetName.contains("13"))
                        workbook.setPrintArea(i, "$A$1:$G$19");
                    else if (sheetName.contains("12"))
                        workbook.setPrintArea(i, "$A$1:$G$32");
                    else if (sheetName.contains("11"))
                        workbook.setPrintArea(i, "$A$1:$D$23");
                    else if (sheetName.contains("10"))
                        workbook.setPrintArea(i, "$A$1:$D$17");
                    else if (sheetName.contains("9"))
                        workbook.setPrintArea(i, "$A$1:$D$18");
                    else if (sheetName.contains("8"))
                        workbook.setPrintArea(i, "$A$1:$G$21");
                    else if (sheetName.contains("7"))
                        workbook.setPrintArea(i, "$A$1:$G$23");
                    else if (sheetName.contains("6"))
                        workbook.setPrintArea(i, "$A$1:$D$19");
                    else if (sheetName.contains("5"))
                        workbook.setPrintArea(i, "$A$1:$G$28");
                    else if (sheetName.contains("4")){
                        workbook.setPrintArea(i, "$A$1:$G$31");
                        ps.setLandscape(true);
                    }
                    else if (sheetName.contains("3d"))
                        workbook.setPrintArea(i, "$A$1:$F$25");
                    else if (sheetName.contains("3c")){
                        workbook.setPrintArea(i, "$A$1:$E$24");
                        ps.setLandscape(false);
                    }
                    else if (sheetName.contains("3b")){
                        workbook.setPrintArea(i, "$A$1:$I$20");
                        ps.setLandscape(true);
                    }
                    else if (sheetName.contains("3a")){
                        workbook.setPrintArea(i, "$A$1:$H$27");
                        ps.setLandscape(true);
                    }
                    else if (sheetName.contains("r4"))
                        workbook.setPrintArea(i, "$A$1:$F$54");
                    else if (sheetName.contains("r2"))
                        workbook.setPrintArea(i, "$A$1:$T$60");
                    else if (sheetName.contains("r3"))
                        workbook.setPrintArea(i, "$A$1:$F$50");
                    else if (sheetName.contains("r1"))
                        workbook.setPrintArea(i, "$A$1:$AN$83");
                    else if (sheetName.contains("2"))
                        workbook.setPrintArea(i, "$A$1:$D$13");
                    else if (sheetName.contains("1"))
                        workbook.setPrintArea(i, "$A$1:$F$43");
                    else if (sheetName.contains("flux"))
                        workbook.setPrintArea(i, "$A$1:$F$36");
                    else if (sheetName.contains("compte"))
                        workbook.setPrintArea(i, "$A$1:$F$48");
                    else if (sheetName.contains("bilan")){
                        workbook.setPrintArea(i, "$A$1:$L$36");
                        ps.setLandscape(true);
                    }


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

            return pdfBytes;
        } catch (Exception e) {
            return null;
        }
    }


}
