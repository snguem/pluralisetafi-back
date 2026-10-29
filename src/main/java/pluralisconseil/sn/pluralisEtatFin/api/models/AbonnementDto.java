package pluralisconseil.sn.pluralisEtatFin.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbonnementDto implements Serializable {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Long societeId;
    private Long planId;
    private int nbMois;
    private boolean hybride;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean active;
    private java.math.BigDecimal montant;
    private LocalDate dateVente;
}
