package pluralisconseil.sn.pluralisEtatFin.data.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entreprise")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Entreprise implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean active=true;

    @CreationTimestamp
    @Temporal(TemporalType.DATE)
    @Column(updatable = false)
    private Date createAt = Date.valueOf(LocalDate.now());

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

//
    @Column(unique = true)
    private String name;
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


    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActiviteEntreprise> activiteEntreprises = new ArrayList<>();

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActionaireEntreprise> actionaireEntreprises = new ArrayList<>();

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DirigentEntreprise> dirigentEntreprises = new ArrayList<>();

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GerantEntreprise> gerantEntreprises = new ArrayList<>();

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BanqueEntreprise> banqueEntreprises = new ArrayList<>();

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EtatFinancier> etatFinanciers = new ArrayList<>();
}
