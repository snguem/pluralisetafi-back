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

@Entity
@Table(name = "etats_financier")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class EtatFinancier implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected Boolean active=true;

    @CreationTimestamp
    @Temporal(TemporalType.DATE)
    @Column(updatable = false)
    protected Date createAt = Date.valueOf(LocalDate.now());

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private ModelExcel model;


}
