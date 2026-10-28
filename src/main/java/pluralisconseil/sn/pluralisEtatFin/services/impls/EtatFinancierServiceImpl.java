package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EtatFinancierMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EtatFinancierRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EtatFinancierServiceImpl implements EtatFinancierService {
    private final EtatFinancierRepository repository;
    private final EtatFinancierMapper mapper;
    private final EntrepriseService entrepriseService;

    @Override
    public EtatFinancierDto create(EtatFinancierDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        if (dto.getEntreprise() != null && dto.getEntreprise().getId_() != null) {
            entity.setEntrepriseRef(entrepriseService.getEntity(dto.getEntreprise().getId_()));
        }
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public EtatFinancierDto update(EtatFinancierDto dto) {
        var entity_database = repository.findById(dto.getId()).get();
        var entity = mapper.asEntity(dto);
        // Preserve the real Entreprise relation: mapper.asEntity(dto) builds a fresh entity via
        // the Lombok builder, which has no way to populate entrepriseRef from the DTO (the DTO
        // only carries the embedded snapshot). Same "lose a relation on save" risk Task 4 fixed
        // for Entreprise.societe.
        entity.setEntrepriseRef(entity_database.getEntrepriseRef());
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
        var q = QEtatFinancier.etatFinancier;
        booleanBuilder.and(q.entreprise.id_.eq(entreprise_id));
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }

    @Override
    public Page<EtatFinancierDto> getAllBySociete(Long societeId, Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        var q = QEtatFinancier.etatFinancier;
        booleanBuilder.and(q.entrepriseRef.societe.id.eq(societeId));
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }

    @Override
    public void deleteAllBySociete(Long societeId) {
        repository.deleteByEntrepriseRef_Societe_Id(societeId);
    }

    @Override
    public EtatFinancier getEntity(Long id) {
        return repository.findById(id).get();
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
                booleanBuilder.and(qEntity.entreprise.id_.eq(Long.parseLong(searchParams.get("entreprise_id"))));

        }
    }
}
