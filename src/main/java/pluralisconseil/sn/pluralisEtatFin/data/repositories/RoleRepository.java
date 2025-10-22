package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AppRole;

@Repository
public interface RoleRepository extends JpaRepository<AppRole, Long>, QuerydslPredicateExecutor<AppRole> {
    AppRole findByRoleName(String name);

}
