package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;

import java.util.List;
import java.util.Map;

public interface ActiviteEntrepriseService extends Service<ActiviteEntrepriseDto> {
    Page<ActiviteEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);

    List<ActiviteEntrepriseDto> getListByEntreprise(Long id);
}
