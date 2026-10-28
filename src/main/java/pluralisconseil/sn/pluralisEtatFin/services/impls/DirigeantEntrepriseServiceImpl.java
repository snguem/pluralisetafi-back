package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.DirigeantEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.DirigentEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.DirigeantEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DirigeantEntrepriseService;

@Service
@RequiredArgsConstructor
public class DirigeantEntrepriseServiceImpl
        extends AbstractEntrepriseChildService<DirigentEntreprise, DirigeantEntrepriseDto>
        implements DirigeantEntrepriseService {

    private final DirigeantEntrepriseRepository repository;
    private final DirigeantEntrepriseMapper mapper;

    @Override
    protected DirigeantEntrepriseRepository repository() {
        return repository;
    }

    @Override
    protected DirigeantEntrepriseMapper mapper() {
        return mapper;
    }
}
