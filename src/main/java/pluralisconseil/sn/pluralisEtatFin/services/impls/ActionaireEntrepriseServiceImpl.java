package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ActionaireEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActionaireEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ActionaireEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActionaireEntrepriseService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActionaireEntrepriseServiceImpl implements ActionaireEntrepriseService {
    private final ActionaireEntrepriseRepository repository;
    private final ActionaireEntrepriseMapper mapper;

    @Override
    public ActionaireEntrepriseDto create(ActionaireEntrepriseDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public ActionaireEntrepriseDto update(ActionaireEntrepriseDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("ActionaireEntreprise not found");
        }
        repository.deleteById(id);
    }

    @Override
    public ActionaireEntrepriseDto get(Long id) {
        var entity = repository.findById(id);
        return mapper.asDto(entity.get());
    }

    @Override
    public Page<ActionaireEntrepriseDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }


    @Override
    public Page<ActionaireEntrepriseDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        var q = QEntreprise.entreprise;
        booleanBuilder.and(q.id.eq(entreprise_id));
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }


    @Override
    public long countAll() {
        return repository.count();
    }

    private void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
        if (Objects.nonNull(searchParams)) {
//            var qEntity = QActionaireEntreprise.user;
        }
    }
}
