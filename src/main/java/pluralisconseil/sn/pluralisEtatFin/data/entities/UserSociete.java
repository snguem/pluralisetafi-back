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
 * Links a Keycloak user (by id) to the {@code Societe} they belong to, and the role they hold
 * within it. {@code societeId} is a plain FK column rather than a {@code @ManyToOne}, since
 * nothing currently needs to navigate from a {@code UserSociete} to a loaded {@code Societe}.
 */
@Entity
@Table(name = "user_societe")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class UserSociete implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keycloakUserId;

    @Column(nullable = false)
    private Long societeId;

    @Column(nullable = false)
    private String role;

    @CreationTimestamp
    @Temporal(TemporalType.DATE)
    @Column(updatable = false)
    private Date createAt = Date.valueOf(LocalDate.now());

    @UpdateTimestamp
    private Date updateAt;
}
