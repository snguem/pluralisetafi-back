package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.ModelPdfPageDto;
import pluralisconseil.sn.pluralisEtatFin.helpers.PdfPageSettings;

import java.util.List;
import java.util.Map;

/** The PDF print settings of the sheets of an Excel model. */
public interface ModelPdfPageService {

    List<ModelPdfPageDto> getAll(Long modelId);

    ModelPdfPageDto create(Long modelId, ModelPdfPageDto dto);

    ModelPdfPageDto update(Long modelId, Long id, ModelPdfPageDto dto);

    void delete(Long modelId, Long id);

    /** Adds the sheets of the list, and updates the ones the model already has (same sheet name). */
    ImportResult importAll(Long modelId, List<ModelPdfPageDto> dtos);

    /** Fills the model with the default rules for each of its sheets that has no settings yet. */
    List<ModelPdfPageDto> seedDefaults(Long modelId) throws Exception;

    /**
     * Sets, for every sheet of the model's Excel file that holds something, the area and the orientation computed
     * from its real content. It replaces the settings the sheets already have.
     */
    List<ModelPdfPageDto> fitToContent(Long modelId) throws Exception;

    /** The names of the sheets of the model's Excel file. */
    List<String> getSheetNames(Long modelId) throws Exception;

    /** The settings the model sets itself, by lower-case sheet name. */
    Map<String, PdfPageSettings> settingsByModel(Long modelId);

    record ImportResult(int created, int updated) {
    }
}
