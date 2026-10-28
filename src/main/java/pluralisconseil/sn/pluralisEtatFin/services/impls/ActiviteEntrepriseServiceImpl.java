package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ActiviteEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActiviteEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ActiviteEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActiviteEntrepriseService;

@Service
@RequiredArgsConstructor
public class ActiviteEntrepriseServiceImpl
        extends AbstractEntrepriseChildService<ActiviteEntreprise, ActiviteEntrepriseDto>
        implements ActiviteEntrepriseService {

    private final ActiviteEntrepriseRepository repository;
    private final ActiviteEntrepriseMapper mapper;

    @Override
    protected ActiviteEntrepriseRepository repository() {
        return repository;
    }

    @Override
    protected ActiviteEntrepriseMapper mapper() {
        return mapper;
    }
}
