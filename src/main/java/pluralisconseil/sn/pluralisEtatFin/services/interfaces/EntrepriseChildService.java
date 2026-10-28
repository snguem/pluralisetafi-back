package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Common contract shared by every "X of an Entreprise" service (dirigeants, banques,
 * actionnaires, activites, gerants): plain CRUD plus listing by the owning entreprise.
 */
public interface EntrepriseChildService<D> extends Service<D> {
    Page<D> getAllByEntreprise(long entrepriseId, Map<String, String> searchParams, Pageable pageable);

    List<D> getListByEntreprise(Long entrepriseId);
}
