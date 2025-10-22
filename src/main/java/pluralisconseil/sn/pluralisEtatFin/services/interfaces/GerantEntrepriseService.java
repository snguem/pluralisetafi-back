package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActionaireEntreprise;

import java.util.Map;

public interface GerantEntrepriseService extends Service<GerantEntrepriseDto> {
    Page<GerantEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);
}
