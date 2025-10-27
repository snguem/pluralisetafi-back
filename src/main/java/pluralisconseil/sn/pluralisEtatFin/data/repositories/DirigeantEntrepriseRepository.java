package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActionaireEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.DirigentEntreprise;

import java.util.List;

@Repository
public interface DirigeantEntrepriseRepository extends JpaRepository<DirigentEntreprise, Long>, QuerydslPredicateExecutor<DirigentEntreprise> {
    Page<DirigentEntreprise> findAllByEntreprise_Id(long id, Pageable page, BooleanBuilder builder);

    List<DirigentEntreprise> findAllByEntreprise_Id(Long entrepriseId);
}
