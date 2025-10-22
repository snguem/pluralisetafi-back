package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.RoleMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.RoleDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QAppRole;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.RoleRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.RoleService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository repository;
    private final RoleMapper mapper;

    @Override
    public RoleDto create(RoleDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public RoleDto update(RoleDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("Role not found");
        }
        repository.deleteById(id);
    }

    @Override
    public RoleDto get(Long id) {
        var entity = repository.findById(id);
        return mapper.asDto(entity.get());
    }

    @Override
    public Page<RoleDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }

    @Override
    public List<RoleDto> getAllVisible() {
        var booleanBuilder = new BooleanBuilder();
        var q= QAppRole.appRole;
        booleanBuilder.and(q.roleName.containsIgnoreCase("Developper"));
        return StreamSupport.stream(
                    repository.findAll(booleanBuilder).spliterator(), false
                )
                .map(mapper::asDto)
                .collect(Collectors.toList());
    }



    private void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
        if (Objects.nonNull(searchParams)) {
//            var qEntity = QRole.user;
        }
    }

}
