package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pluralisconseil.sn.pluralisEtatFin.data.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {
    Optional<User> findByLogin(String mail);

    @Modifying
    @Transactional
    @Query("update User u set u.name=:name, u.login=:login where u.id=:id")
    int updateById(@Param("id") Long id, @Param("name") String name, @Param("login") String login);

    @Modifying
    @Transactional
    @Query("update User u set u.password=:password where u.login=:username")
    int updatePasswordByUsername(@Param("username") String username, @Param("password") String password);
}
