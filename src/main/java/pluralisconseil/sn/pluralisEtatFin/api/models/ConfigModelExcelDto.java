package pluralisconseil.sn.pluralisEtatFin.api.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeAttributIsConfig;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;

import java.io.Serializable;
import java.sql.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // lors de la transformation en json, les attributs ayant une valeur null seront ignores
@JsonIgnoreProperties(ignoreUnknown = true) // lors de la transformation du json en object, les cles inexistantes dans l'object mais presente dans le json doivent etre ignore
public class ConfigModelExcelDto implements Serializable {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date createAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date updatedAt;

    private Boolean active;

    private String page;
    private String field;
    private String class_fields_if_list;
    private String codeExcel;
    private int l_number;
    private int c_number;

    private TypeConfig typeConfig;
    private TypeAttributIsConfig typeAttributIsConfigConfig;

    private String model_name;
    private Long model_id;

}
