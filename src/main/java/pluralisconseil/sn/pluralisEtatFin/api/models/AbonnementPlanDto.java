package pluralisconseil.sn.pluralisEtatFin.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbonnementPlanDto implements Serializable {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private String nom;
    private int entrepriseQuota;
    private boolean hybrideInclus;
    private boolean hybrideDisponibleEnOption;
    private BigDecimal hybrideSupplementPrix;
    private BigDecimal prixParMois;
    private boolean active;
}
