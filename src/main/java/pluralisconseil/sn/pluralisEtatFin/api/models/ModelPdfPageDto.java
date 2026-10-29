package pluralisconseil.sn.pluralisEtatFin.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** The PDF print settings of one sheet of a model; the same shape is exported and imported (without the id). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelPdfPageDto implements Serializable {
    private Long id;
    private String sheetName;
    private String printArea;
    private boolean landscape;
}
