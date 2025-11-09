package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EntrepriseServiceImpl implements EntrepriseService {
    private final EntrepriseRepository repository;
    private final EntrepriseMapper mapper;

    @Override
    public EntrepriseDto create(EntrepriseDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        entity.getSubstitute().setSous_controle_public(false);
        entity.getSubstitute().setSous_controle_prive_etranger(false);
        entity.getSubstitute().setSous_controle_prive_national(false);
        var entitySave = repository.save(entity);
        var dto_= mapper.asDto(entitySave);
//        dto_.setNbr_etat_fin(0);
        return dto_;
    }

    @Override
    public EntrepriseDto update(EntrepriseDto dto) {
        var entity_database = repository.findById(dto.getId()).get();
        var entity = mapper.asEntity(dto);

        entity.setSubstitute(dto.getSubstitute());
        entity.setCreateAt(entity_database.getCreateAt());
        entity.setUpdatedAt(entity_database.getUpdatedAt());
        entity.setActiviteEntreprises(entity_database.getActiviteEntreprises());
        entity.setActionaireEntreprises(entity_database.getActionaireEntreprises());
        entity.setDirigentEntreprises(entity_database.getDirigentEntreprises());
        entity.setGerantEntreprises(entity_database.getGerantEntreprises());
        entity.setBanqueEntreprises(entity_database.getBanqueEntreprises());

        var entitySave = repository.save(entity);
        var dto_ =mapper.asDto(entitySave);
//        dto_.setNbr_etat_fin(entitySave.getEtatFinanciers().size());
        return dto_;
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("Entreprise not found");
        }
        repository.deleteById(id);
    }

    @Override
    public EntrepriseDto getName(String name) {
        var entity = repository.findBySubstitute_NameEntrepriseEqualsIgnoreCase(name);
        var dto= mapper.asDto(entity);
//        dto.setNbr_etat_fin(entity.getEtatFinanciers().size());
        return dto;
    }

    @Override
    public EntrepriseDto get(Long id) {
        var entity = repository.findById(id).get();
        var dto= mapper.asDto(entity);
//        dto.setNbr_etat_fin(entity.getEtatFinanciers().size());
        return dto;
    }

    @Override
    public Entreprise getEntity(Long id) {
        var entity = repository.findById(id).get();
//        dto.setNbr_etat_fin(entity.getEtatFinanciers().size());
        return entity;
    }

    @Override
    public Page<EntrepriseDto> getAll(Map<String, String> searchParams, Pageable pageable) {
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
            var qEntity = QEntreprise.entreprise;

            if (searchParams.containsKey("name"))
                booleanBuilder.and(qEntity.substitute.nameEntreprise.containsIgnoreCase(searchParams.get("name")));
        }
    }

}
