package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ConfigModelEntrepriseField;

@Repository
public interface ConfigModelExcelRepository extends JpaRepository<ConfigModelEntrepriseField, Long>, QuerydslPredicateExecutor<ConfigModelEntrepriseField> {
    Page<ConfigModelEntrepriseField> findAllByModel_Id(long modelId, BooleanBuilder bolean, Pageable page);
}
