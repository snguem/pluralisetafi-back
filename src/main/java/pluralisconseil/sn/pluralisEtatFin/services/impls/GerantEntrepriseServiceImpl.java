package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.GerantEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.GerantEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.GerantEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.GerantEntrepriseService;

@Service
@RequiredArgsConstructor
public class GerantEntrepriseServiceImpl
        extends AbstractEntrepriseChildService<GerantEntreprise, GerantEntrepriseDto>
        implements GerantEntrepriseService {

    private final GerantEntrepriseRepository repository;
    private final GerantEntrepriseMapper mapper;

    @Override
    protected GerantEntrepriseRepository repository() {
        return repository;
    }

    @Override
    protected GerantEntrepriseMapper mapper() {
        return mapper;
    }
}
