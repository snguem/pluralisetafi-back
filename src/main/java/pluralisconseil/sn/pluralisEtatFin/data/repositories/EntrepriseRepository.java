package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long>, QuerydslPredicateExecutor<Entreprise> {
    Entreprise findByNameEqualsIgnoreCase(String name);
}
