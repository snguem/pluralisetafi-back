package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocieteRepository extends JpaRepository<UserSociete, Long> {

    Optional<UserSociete> findByKeycloakUserId(String keycloakUserId);

    List<UserSociete> findBySocieteId(Long societeId);
}
