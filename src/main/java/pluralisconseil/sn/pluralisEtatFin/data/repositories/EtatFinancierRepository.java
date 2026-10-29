package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;

@Repository
public interface EtatFinancierRepository extends JpaRepository<EtatFinancier, Long>, QuerydslPredicateExecutor<EtatFinancier> {

    void deleteByEntrepriseRef_Societe_Id(Long societeId);

    long countByEntrepriseRef_Societe_Id(Long societeId);

    List<EtatFinancier> findTop5ByEntrepriseRef_Societe_IdOrderByIdDesc(Long societeId);
}
