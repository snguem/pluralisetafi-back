package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ActiviteEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ActiviteEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActiviteEntrepriseService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActiviteEntrepriseServiceImpl implements ActiviteEntrepriseService {
    private final ActiviteEntrepriseRepository repository;
    private final ActiviteEntrepriseMapper mapper;

    @Override
    public ActiviteEntrepriseDto create(ActiviteEntrepriseDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public ActiviteEntrepriseDto update(ActiviteEntrepriseDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("ActiviteEntreprise not found");
        }
        repository.deleteById(id);
    }

    @Override
    public ActiviteEntrepriseDto get(Long id) {
        var entity = repository.findById(id);
        return mapper.asDto(entity.get());
    }

    @Override
    public Page<ActiviteEntrepriseDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }


    @Override
    public Page<ActiviteEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        var q = QEntreprise.entreprise;
        booleanBuilder.and(q.id.eq(entreprise_id));
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }

    private void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
        if (Objects.nonNull(searchParams)) {
//            var qEntity = QActiviteEntreprise.user;
        }
    }
}
