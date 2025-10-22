package pluralisconseil.sn.pluralisEtatFin.api.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestPart;

import java.io.Serializable;
import java.sql.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // lors de la transformation en json, les attributs ayant une valeur null seront ignores
@JsonIgnoreProperties(ignoreUnknown = true) // lors de la transformation du json en object, les cles inexistantes dans l'object mais presente dans le json doivent etre ignore
public class EtatFinancierDto implements Serializable {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date createAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date updateAt;
    private Boolean active;

    //
    private String name;
    private String excelPath;
    private String centre_depot;
    private Date execice_actuel_debut_date;
    private Date date_arret_effectif_comptes;
    private Date execice_actuel_fin_date;
    private Date execice_actuel_clos_date;
    private Date execice_precedent_clos_date;
    private Date date_signature;
    private int duree_actuel_en_mois;
    private int duree_precedent_en_mois;
    private int annee_n;
    private int annee_n_1;

    private String name_signataire;
    private String qualite_signataire;

    //    check
    private boolean is_fiche_identification;
    private boolean is_bilan;
    private boolean is_compte_resultat;
    private boolean is_tableau_flux;
    private boolean is_note_annexes;


    private String entreprise_name;
    private Long entrepriseId;
    private String modelExcel_name;
    private Long modelExcel_id;

}
