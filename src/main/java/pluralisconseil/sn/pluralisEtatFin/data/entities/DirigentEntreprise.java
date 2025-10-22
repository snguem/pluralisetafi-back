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
@Table(name = "dirigeants_entreprise")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class DirigentEntreprise implements Serializable {

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

    private String name;
    private String surname;
    private String qualite;
    private String adresse;
    private String identification_fiscale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;


}
