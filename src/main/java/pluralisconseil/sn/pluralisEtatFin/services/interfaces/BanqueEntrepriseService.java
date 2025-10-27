package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.BanqueEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;

import java.util.List;
import java.util.Map;

public interface BanqueEntrepriseService extends Service<BanqueEntrepriseDto> {
    Page<BanqueEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);

    List<BanqueEntrepriseDto> getListByEntreprise(Long id);
}
