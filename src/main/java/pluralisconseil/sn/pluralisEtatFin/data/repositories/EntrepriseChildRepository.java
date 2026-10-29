package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Common shape shared by every "X of an Entreprise" repository (dirigeants, banques,
 * actionnaires, activites, gerants): plain CRUD plus the two ways of listing by the owning
 * entreprise (paginated+filterable, and a plain list).
 */
@NoRepositoryBean
public interface EntrepriseChildRepository<E> extends JpaRepository<E, Long>, QuerydslPredicateExecutor<E> {
    Page<E> findAllByEntreprise_Id(long entrepriseId, Pageable page);

    List<E> findAllByEntreprise_Id(long entrepriseId);

    Page<E> findAllByEntreprise_Societe_Id(long societeId, Pageable page);
}
