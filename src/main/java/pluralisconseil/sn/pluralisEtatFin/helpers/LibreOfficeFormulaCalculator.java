package pluralisconseil.sn.pluralisEtatFin.helpers;

import lombok.RequiredArgsConstructor;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Recalculates the formulas of an xlsx by round-tripping it through LibreOffice, which
 * computes them on load and writes the values back.
 */
@RequiredArgsConstructor
@Service
public class LibreOfficeFormulaCalculator implements ExcelFormulaCalculator {
    private final DocumentConverter converter;
    private final HelperService helperService;

    @Override
    public void calculateFormule(String path) throws Exception {
        if (!helperService.isExist(path))
            throw new pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException("Le fichier est introuvable");

        File recalculated = File.createTempFile("etat_fin_calc", ".xlsx");
        try {
            converter
                    .convert(new File(path))
                    .as(DefaultDocumentFormatRegistry.XLSX)
                    .to(recalculated)
                    .as(DefaultDocumentFormatRegistry.XLSX)
                    .execute();
            Files.copy(recalculated.toPath(), new File(path).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            recalculated.delete();
        }
    }
}
