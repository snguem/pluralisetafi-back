package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.BanqueEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.DirigentEntreprise;

import java.util.List;

@Repository
public interface BanqueEntrepriseRepository extends JpaRepository<BanqueEntreprise, Long>, QuerydslPredicateExecutor<BanqueEntreprise> {
    Page<BanqueEntreprise> findAllByEntreprise_Id(long id, Pageable page, BooleanBuilder builder);

    List<BanqueEntreprise> findAllByEntreprise_Id(long id);
}
