package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;

@Repository
public interface AbonnementPlanRepository extends JpaRepository<AbonnementPlan, Long> {
}
