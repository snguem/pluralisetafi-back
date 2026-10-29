package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementPlanMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.AbonnementPlanDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementPlanService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbonnementPlanServiceImpl implements AbonnementPlanService {

    private final AbonnementPlanRepository repository;
    private final AbonnementPlanMapper mapper;

    @Override
    public AbonnementPlanDto create(String nom, int entrepriseQuota, boolean hybrideInclus,
                                     boolean hybrideDisponibleEnOption, BigDecimal hybrideSupplementPrix,
                                     BigDecimal prixParMois, boolean active) {
        AbonnementPlan entity = AbonnementPlan.builder()
                .nom(nom)
                .entrepriseQuota(entrepriseQuota)
                .hybrideInclus(hybrideInclus)
                .hybrideDisponibleEnOption(hybrideDisponibleEnOption)
                .hybrideSupplementPrix(hybrideSupplementPrix)
                .prixParMois(prixParMois)
                .active(active)
                .build();
        return mapper.asDto(repository.save(entity));
    }

    @Override
    public AbonnementPlanDto update(Long id, String nom, int entrepriseQuota, boolean hybrideInclus,
                                     boolean hybrideDisponibleEnOption, BigDecimal hybrideSupplementPrix,
                                     BigDecimal prixParMois, boolean active) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AbonnementPlan not found: " + id));
        entity.setNom(nom);
        entity.setEntrepriseQuota(entrepriseQuota);
        entity.setHybrideInclus(hybrideInclus);
        entity.setHybrideDisponibleEnOption(hybrideDisponibleEnOption);
        entity.setHybrideSupplementPrix(hybrideSupplementPrix);
        entity.setPrixParMois(prixParMois);
        entity.setActive(active);
        return mapper.asDto(repository.save(entity));
    }

    @Override
    public AbonnementPlanDto get(Long id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AbonnementPlan not found: " + id));
        return mapper.asDto(entity);
    }

    @Override
    public List<AbonnementPlanDto> getAll() {
        return mapper.parse(repository.findAll());
    }

    @Override
    public AbonnementPlanDto setActive(Long id, boolean active) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AbonnementPlan not found: " + id));
        entity.setActive(active);
        return mapper.asDto(repository.save(entity));
    }
}
