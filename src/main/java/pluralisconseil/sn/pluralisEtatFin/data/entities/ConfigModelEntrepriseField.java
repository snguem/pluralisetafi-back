package pluralisconseil.sn.pluralisEtatFin.data.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeAttributIsConfig;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Table(name = "config_model")
@Entity
@Data
@NoArgsConstructor
public class ConfigModelEntrepriseField implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

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