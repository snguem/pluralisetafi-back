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

    Page<D> getAllBySociete(long societeId, Pageable pageable);

    /** Id of the entreprise owning the given child record, or {@code null} if it has none. */
    Long getEntrepriseId(Long id);
}
