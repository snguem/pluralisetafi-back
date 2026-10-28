package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.BanqueEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.BanqueEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.BanqueEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.BanqueEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.BanqueEntrepriseService;

@Service
@RequiredArgsConstructor
public class BanqueEntrepriseServiceImpl
        extends AbstractEntrepriseChildService<BanqueEntreprise, BanqueEntrepriseDto>
        implements BanqueEntrepriseService {

    private final BanqueEntrepriseRepository repository;
    private final BanqueEntrepriseMapper mapper;

    @Override
    protected BanqueEntrepriseRepository repository() {
        return repository;
    }

    @Override
    protected BanqueEntrepriseMapper mapper() {
        return mapper;
    }
}
