package pluralisconseil.sn.pluralisEtatFin.api.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

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

    //
    private String name;
    private int nbr_etat_fin;
    private String sigle_usuel;
    private String adresse;
    private String contact;
    private String mail;
    private String greffe;
    private String registre_commerce;
    private String caisse_sociale;
    private String code_importeur;

    // personne a contacter en cas de demande d'informations
    private String name_to_ask;
    private String adresse_to_ask;
    private String qualite_to_ask;
    // professionel cabinet
    private String name_ionecca;
    private String adresse_ionecca;
    private String contact_ionecca;
    // signataire
    private String name_signataire;
    private String qualite_signataire;
    // fiche identification et divers
    private int forme_juridique;
    private int regime_fiscal;
    private int siege_social;
    private int nbr_etablissement_pays;
    private int nbr_etablissiement_pays_distinct;
    private int premier_annee_exercice;
    // check
    private boolean is_sous_controle_public;
    private boolean is_sous_controle_prive_national;
    private boolean is_sous_controle_prive_etranger;
    // infos suppl
    private String identification_fiscale;
    private String repertoire_entreprise;
    private String code_activite_principale;
    private String designation_entreprise;
    // boite postale
    private String code;
    private String boite;
    private String ville;

//


}
