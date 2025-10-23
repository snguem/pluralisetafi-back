package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ConfigModelExcelMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QConfigModelEntrepriseField;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QModelExcel;
import pluralisconseil.sn.pluralisEtatFin.data.enums.TypeConfig;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ConfigModelExcelRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ConfigModelExcelService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ConfigModelExcelServiceImpl implements ConfigModelExcelService {
    private final ConfigModelExcelRepository repository;
    private final ConfigModelExcelMapper mapper;

    @Override
    public ConfigModelExcelDto create(ConfigModelExcelDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public ConfigModelExcelDto update(ConfigModelExcelDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("ConfigModelExcel not found");
        }
        repository.deleteById(id);
    }

    @Override
    public ConfigModelExcelDto get(Long id) {
        var entity = repository.findById(id);
        return mapper.asDto(entity.get());
    }

    @Override
    public Page<ConfigModelExcelDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }

    @Override
    public List<ConfigModelExcelDto> getAll(Map<String, String> searchParams) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return  StreamSupport.stream(
                        repository.findAll(booleanBuilder).spliterator(), false
                )
                .map(mapper::asDto)
                .collect(Collectors.toList());
    }


    @Override
    public Page<ConfigModelExcelDto> getAllByModelExcel(long model_id, Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        var q = QConfigModelEntrepriseField.configModelEntrepriseField;
        booleanBuilder.and(q.model.id.eq(model_id));
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
            var qEntity = QConfigModelEntrepriseField.configModelEntrepriseField;

            if (searchParams.containsKey("type_config"))
                booleanBuilder.and(qEntity.typeConfig.eq(TypeConfig.valueOf(searchParams.get("type_config"))));
        }
    }
}
