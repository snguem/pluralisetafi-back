package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ModelExcelMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelExcel;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ModelExcelRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ModelExcelServiceImpl implements ModelExcelService {
    private final ModelExcelRepository repository;
    private final ModelExcelMapper mapper;

    @Override
    public ModelExcelDto create(ModelExcelDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public ModelExcelDto update(ModelExcelDto dto) {
        var entity_database = repository.findById(dto.getId()).get();
        var entity = mapper.asEntity(dto);
        entity.setConfigs(entity_database.getConfigs());
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("ModelExcel not found");
        }
        repository.deleteById(id);
    }

    @Override
    public ModelExcelDto get(Long id) {
        var entity = repository.findById(id);
        var dto= mapper.asDto(entity.get());
        dto.setUpdateAt(entity.get().getUpdatedAt());
        return dto;
    }

    @Override
    public ModelExcelDto getName(String name) {
        var entity = repository.findByName(name);
        var dto= mapper.asDto(entity);
        dto.setUpdateAt(entity.getUpdatedAt());
        return dto;
    }

    @Override
    public Page<ModelExcelDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
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
//            var qEntity = QModelExcel.user;
        }
    }
}
