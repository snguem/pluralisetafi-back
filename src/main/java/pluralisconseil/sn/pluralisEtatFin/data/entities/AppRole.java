package pluralisconseil.sn.pluralisEtatFin.data.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "roles")
@Builder
public class AppRole {
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

    @Column(nullable = false,unique = true)
     private String roleName;

     @ManyToMany(fetch = FetchType.LAZY,mappedBy = "roles")
     List<User> users;

}
