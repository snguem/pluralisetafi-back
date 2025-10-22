package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelExcel;

import java.util.Optional;

@Repository
public interface ModelExcelRepository extends JpaRepository<ModelExcel, Long>, QuerydslPredicateExecutor<ModelExcel> {

    ModelExcel findByName(String name);
}
