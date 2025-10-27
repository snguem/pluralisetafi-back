package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;

public interface EntrepriseService extends Service<EntrepriseDto> {
    EntrepriseDto getName(String name);
    Entreprise getEntity(Long id);
}
