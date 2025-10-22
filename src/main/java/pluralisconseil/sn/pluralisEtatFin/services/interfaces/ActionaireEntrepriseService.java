package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActionaireEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActionaireEntreprise;

import java.util.Map;

public interface ActionaireEntrepriseService extends Service<ActionaireEntrepriseDto> {
    Page<ActionaireEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);
}
