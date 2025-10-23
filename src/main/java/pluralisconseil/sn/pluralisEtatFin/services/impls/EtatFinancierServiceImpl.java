package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EtatFinancierMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EtatFinancierRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EtatFinancierServiceImpl implements EtatFinancierService {
    private final EtatFinancierRepository repository;
    private final EtatFinancierMapper mapper;

    @Override
    public EtatFinancierDto create(EtatFinancierDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public EtatFinancierDto update(EtatFinancierDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("EtatFinancier not found");
        }
        repository.deleteById(id);
    }

    @Override
    public EtatFinancierDto get(Long id) {
        var entity = repository.findById(id);
        return mapper.asDto(entity.get());
    }

    @Override
    public Page<EtatFinancierDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }


    @Override
    public Page<EtatFinancierDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable) {
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
            var qEntity = QEtatFinancier.etatFinancier;

            if (searchParams.containsKey("name"))
                booleanBuilder.and(qEntity.name.containsIgnoreCase(searchParams.get("name")));


            if (searchParams.containsKey("entreprise_id") && Integer.parseInt(searchParams.get("entreprise_id"))!=0)
                booleanBuilder.and(qEntity.entreprise.id.eq(Long.parseLong(searchParams.get("entreprise_id"))));

        }
    }
}
