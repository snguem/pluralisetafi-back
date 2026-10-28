package pluralisconseil.sn.pluralisEtatFin.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A subscription plan in the catalog: how many {@code Entreprise} a {@code Societe} may have
 * under it, and whether the "hybride" capability is bundled in, offered as a paid add-on, or
 * unavailable, at a given monthly price.
 */
@Entity
@Table(name = "abonnement_plan")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class AbonnementPlan implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private int entrepriseQuota;

    @Column(nullable = false)
    private boolean hybrideInclus;

    @Column(nullable = false)
    private boolean hybrideDisponibleEnOption;

    private BigDecimal hybrideSupplementPrix;

    @Column(nullable = false)
    private BigDecimal prixParMois;

    @Column(nullable = false)
    private boolean active;
}
