package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long>, QuerydslPredicateExecutor<Entreprise> {
    Entreprise findBySubstitute_NameEntrepriseEqualsIgnoreCase(String name);

    Entreprise findBySociete_IdAndSubstitute_NameEntrepriseEqualsIgnoreCase(Long societeId, String name);

    long countBySociete_Id(Long societeId);
}
