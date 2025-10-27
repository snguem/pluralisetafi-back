package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActiviteEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;

import java.util.List;

@Repository
public interface ActiviteEntrepriseRepository extends JpaRepository<ActiviteEntreprise, Long>, QuerydslPredicateExecutor<ActiviteEntreprise> {
    Page<ActiviteEntreprise> findAllByEntreprise_Id(long id, Pageable page, BooleanBuilder builder);
    List<ActiviteEntreprise> findAllByEntreprise_Id(long id);
}
