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
import java.time.LocalDate;

/**
 * A {@code Societe}'s subscription to an {@code AbonnementPlan} for a given period.
 * {@code societeId} and {@code planId} are plain FK columns rather than {@code @ManyToOne},
 * same reasoning as {@code UserSociete}: nothing currently needs to navigate from an
 * {@code Abonnement} to a loaded {@code Societe}/{@code AbonnementPlan}.
 *
 * <p>At most one {@code Abonnement} is expected to be {@code active} per {@code societeId} at a
 * time (see {@code AbonnementRepository#findBySocieteIdAndActiveTrue}), so activating a new one
 * for a société that already has an active abonnement deactivates the previous one first.
 */
@Entity
@Table(name = "abonnement")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Abonnement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long societeId;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private int nbMois;

    @Column(nullable = false)
    private boolean hybride;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private boolean active;
}
