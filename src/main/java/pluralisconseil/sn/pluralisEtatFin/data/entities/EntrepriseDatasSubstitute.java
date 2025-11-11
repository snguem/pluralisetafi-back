package pluralisconseil.sn.pluralisEtatFin.data.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class EntrepriseDatasSubstitute {
    private Long id_;
    //
//    @Column(unique = true)
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
//    // signataire
//    private String name_signataire;
//    private String qualite_signataire;
    // fiche identification et divers
    private String forme_juridique;
    private String regime_fiscal;
    private String siege_social;
    private String nbr_etablissement_pays;
    private String nbr_etablissiement_pays_distinct;
    private String premier_annee_exercice;
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

//
}
