package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long>, QuerydslPredicateExecutor<Entreprise> {
    Entreprise findBySubstitute_NameEntrepriseEqualsIgnoreCase(String name);

    Entreprise findBySociete_IdAndSubstitute_NameEntrepriseEqualsIgnoreCase(Long societeId, String name);

    long countBySociete_Id(Long societeId);

    /** The registre de commerce identifies an entreprise across every société. */
    @Query("select e from Entreprise e where lower(e.substitute.registre_commerce) = lower(:registre)")
    java.util.List<Entreprise> findByRegistreCommerce(@Param("registre") String registre);
}
