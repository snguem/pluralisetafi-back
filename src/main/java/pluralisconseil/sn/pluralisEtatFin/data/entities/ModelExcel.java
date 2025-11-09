package pluralisconseil.sn.pluralisEtatFin.data.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.ObjectInputFilter;
import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "models_excel")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class ModelExcel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected Boolean active=true;

    @CreationTimestamp
    @Temporal(TemporalType.DATE)
    @Column(updatable = false)
    protected Date createAt = Date.valueOf(LocalDate.now());

    @UpdateTimestamp
    @Temporal(TemporalType.DATE)
    @Column(name = "updated_at")
    private Date updatedAt;


    @Column(unique = true)
    private String name;

    private String description;
    private int part;

    @Column(unique = true)
    private String excelPath;


    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConfigModelEntrepriseField> configs = new ArrayList<>();

}
