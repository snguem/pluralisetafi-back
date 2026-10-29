package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    Abonnement findBySocieteIdAndActiveTrue(Long societeId);

    long countByActiveTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(LocalDate debut, LocalDate fin);

    long countByActiveTrueAndDateFinBetween(LocalDate from, LocalDate to);

    java.util.List<Abonnement> findByActiveTrue();
}
