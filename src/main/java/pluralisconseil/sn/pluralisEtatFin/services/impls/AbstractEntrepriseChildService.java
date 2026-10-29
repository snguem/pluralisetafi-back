package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntityMapper;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActivableEntity;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseChildRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;

import java.util.List;
import java.util.Map;

/**
 * Shared CRUD + "list by owning entreprise" implementation for every "X of an Entreprise"
 * service (dirigeants, banques, actionnaires, activites, gerants) — previously duplicated
 * almost byte-for-byte across all five.
 *
 * <p>Fixes a bug present in every one of those duplicates: {@code getAllByEntreprise} used to
 * build its QueryDSL predicate against {@code QEntreprise} (the owning Entreprise's own root)
 * instead of the child entity being queried, so the entreprise filter never actually applied.
 * This implementation instead uses the repository's dedicated
 * {@code findAllByEntreprise_Id(entrepriseId, page, builder)} method, which filters correctly.
 */
public abstract class AbstractEntrepriseChildService<E extends ActivableEntity, D> implements EntrepriseChildService<D> {

    protected abstract EntrepriseChildRepository<E> repository();

    protected abstract EntityMapper<D, E> mapper();

    /** Override to add extra filters from {@code searchParams}; no-op by default. */
    protected void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
    }

    @Override
    public D create(D dto) {
        var entity = mapper().asEntity(dto);
        entity.setActive(true);
        var entitySave = repository().save(entity);
        return mapper().asDto(entitySave);
    }

    @Override
    public D update(D dto) {
        var entity = mapper().asEntity(dto);
        var entitySave = repository().save(entity);
        return mapper().asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if (!repository().existsById(id)) {
            throw new RuntimeException("Entity not found");
        }
        repository().deleteById(id);
    }

    @Override
    public D get(Long id) {
        var entity = repository().findById(id);
        return mapper().asDto(entity.get());
    }

    @Override
    public Page<D> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository().findAll(booleanBuilder, pageable)
                .map(mapper()::asDto);
    }

    @Override
    public Page<D> getAllByEntreprise(long entrepriseId, Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository().findAllByEntreprise_Id(entrepriseId, pageable, booleanBuilder)
                .map(mapper()::asDto);
    }

    @Override
    public List<D> getListByEntreprise(Long entrepriseId) {
        return repository().findAllByEntreprise_Id(entrepriseId).stream()
                .map(mapper()::asDto).toList();
    }

    @Override
    public Page<D> getAllBySociete(long societeId, Pageable pageable) {
        return repository().findAllByEntreprise_Societe_Id(societeId, pageable)
                .map(mapper()::asDto);
    }

    @Override
    public Long getEntrepriseId(Long id) {
        var entity = repository().findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));
        return entity.getEntreprise() == null ? null : entity.getEntreprise().getId();
    }

}
