package pluralisconseil.sn.pluralisEtatFin.api.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;

import java.io.Serializable;
import java.sql.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // lors de la transformation en json, les attributs ayant une valeur null seront ignores
@JsonIgnoreProperties(ignoreUnknown = true) // lors de la transformation du json en object, les cles inexistantes dans l'object mais presente dans le json doivent etre ignore
public class EntrepriseDto implements Serializable {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date createAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date updateAt;
    private Boolean active;


    private EntrepriseDatasSubstitute substitute;

}
