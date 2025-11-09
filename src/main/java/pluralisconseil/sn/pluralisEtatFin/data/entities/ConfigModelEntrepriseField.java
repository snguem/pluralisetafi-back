package pluralisconseil.sn.pluralisEtatFin.data.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeAttributIsConfig;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;

import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;

@Table(name = "config_model")
@Entity
@Data
@NoArgsConstructor
@ToString(exclude = {"model"})
public class ConfigModelEntrepriseField implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @CreationTimestamp
    @Temporal(TemporalType.DATE)
    @Column(updatable = false)
    protected Date createAt = Date.valueOf(LocalDate.now());

    @UpdateTimestamp
    @Temporal(TemporalType.DATE)
    @Column(name = "updated_at")
    private Date updatedAt;

    private Boolean active=true;

    private String page;
    private String field;
    private String codeExcel;
    private int l_number;
    private int c_number;
    private String class_fields_if_list;

    @Enumerated(EnumType.STRING)
    private TypeConfig typeConfig;

    @Enumerated(EnumType.STRING)
    private TypeAttributIsConfig typeAttributIsConfigConfig;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private ModelExcel model;
}