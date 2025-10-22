package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;

public interface EntrepriseService extends Service<EntrepriseDto> {
    EntrepriseDto getName(String name);
}
