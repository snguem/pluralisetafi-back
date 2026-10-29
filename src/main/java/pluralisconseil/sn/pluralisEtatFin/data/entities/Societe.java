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

/**
 * A tenant ("société") in the multi-société model: every {@code Entreprise} and its
 * collaborators ultimately belong to exactly one {@code Societe}.
 */
@Entity
@Table(name = "societe")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Societe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private Boolean active = true;

    @CreationTimestamp
    @Temporal(TemporalType.DATE)
    @Column(updatable = false)
    private Date createAt = Date.valueOf(LocalDate.now());

    @UpdateTimestamp
    private Date updateAt;
}
