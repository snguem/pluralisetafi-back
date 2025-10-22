package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;

import java.util.Map;

public interface EtatFinancierService extends Service<EtatFinancierDto> {
    Page<EtatFinancierDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);
}
