package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;

import java.util.Map;

public interface DirigeantEntrepriseService extends Service<DirigeantEntrepriseDto> {
    Page<DirigeantEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);
}
