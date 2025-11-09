package pluralisconseil.sn.pluralisEtatFin.api.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;

import java.io.Serializable;
import java.sql.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // lors de la transformation en json, les attributs ayant une valeur null seront ignores
@JsonIgnoreProperties(ignoreUnknown = true) // lors de la transformation du json en object, les cles inexistantes dans l'object mais presente dans le json doivent etre ignore
public class EntrepriseSubstituteDto implements Serializable {

    private Long id_;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date createAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date updateAt;
    private Boolean active;

    private String nameEntreprise;
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
    //
    private int forme_juridique;
    private int regime_fiscal;
    private int siege_social;
    private int nbr_etablissement_pays;
    private int nbr_etablissiement_pays_distinct;
    private int premier_annee_exercice;
    // check
    private Boolean sous_controle_public;
    private Boolean sous_controle_prive_national;
    private Boolean sous_controle_prive_etranger;
    // infos suppl
    private String identification_fiscale;
    private String repertoire_entreprise;
    private String code_activite_principale;
    private String designation_entreprise;
    // boite postale
    private String code;
    private String boite;
    private String ville;


    public EntrepriseDatasSubstitute asDto(){
        var dto = new EntrepriseDatasSubstitute();
        dto.setId_(id_);
        dto.setNameEntreprise(nameEntreprise);
        dto.setSigle_usuel(sigle_usuel);
        dto.setAdresse(adresse);
        dto.setContact(contact);
        dto.setMail(mail);
        dto.setGreffe(greffe);
        dto.setRegistre_commerce(registre_commerce);
        dto.setCaisse_sociale(caisse_sociale);
        dto.setCode_importeur(code_importeur);
        dto.setName_to_ask(name_to_ask);
        dto.setAdresse_to_ask(adresse_to_ask);
        dto.setQualite_to_ask(qualite_to_ask);
        dto.setName_ionecca(name_ionecca);
        dto.setAdresse_ionecca(adresse_ionecca);
        dto.setContact_ionecca(contact_ionecca);
        dto.setForme_juridique(forme_juridique);
        dto.setRegime_fiscal(regime_fiscal);
        dto.setSiege_social(siege_social);
        dto.setNbr_etablissement_pays(nbr_etablissement_pays);
        dto.setNbr_etablissiement_pays_distinct(nbr_etablissiement_pays_distinct);
        dto.setPremier_annee_exercice(premier_annee_exercice);
        dto.setSous_controle_public(sous_controle_public);
        dto.setSous_controle_prive_national(sous_controle_prive_national);
        dto.setSous_controle_prive_etranger(sous_controle_prive_etranger);
        dto.setIdentification_fiscale(identification_fiscale);
        dto.setRepertoire_entreprise(repertoire_entreprise);
        dto.setCode_activite_principale(code_activite_principale);
        dto.setDesignation_entreprise(designation_entreprise);
        dto.setCode(code);
        dto.setBoite(boite);
        dto.setVille(ville);

        return dto;
    }

}
