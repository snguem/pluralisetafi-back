package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ActionaireEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActionaireEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActionaireEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ActionaireEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActionaireEntrepriseService;

@Service
@RequiredArgsConstructor
public class ActionaireEntrepriseServiceImpl
        extends AbstractEntrepriseChildService<ActionaireEntreprise, ActionaireEntrepriseDto>
        implements ActionaireEntrepriseService {

    private final ActionaireEntrepriseRepository repository;
    private final ActionaireEntrepriseMapper mapper;

    @Override
    protected ActionaireEntrepriseRepository repository() {
        return repository;
    }

    @Override
    protected ActionaireEntrepriseMapper mapper() {
        return mapper;
    }
}
