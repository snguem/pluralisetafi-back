package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {
    Optional<User> findByLogin(String mail);
}
