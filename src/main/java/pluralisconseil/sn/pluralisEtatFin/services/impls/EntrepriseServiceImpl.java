package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.EntrepriseExportDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseLockedException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseQuotaExceededException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActionaireEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActiviteEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.BanqueEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DirigeantEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.GerantEntrepriseService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EntrepriseServiceImpl implements EntrepriseService {
    private final EntrepriseRepository repository;
    private final EntrepriseMapper mapper;
    private final AbonnementService abonnementService;
    private final AbonnementPlanRepository abonnementPlanRepository;
    private final DirigeantEntrepriseService dirigeantEntrepriseService;
    private final BanqueEntrepriseService banqueEntrepriseService;
    private final ActionaireEntrepriseService actionaireEntrepriseService;
    private final ActiviteEntrepriseService activiteEntrepriseService;
    private final GerantEntrepriseService gerantEntrepriseService;

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
    public EntrepriseDto create(EntrepriseDto dto, Long societeId) throws EntrepriseQuotaExceededException, EntrepriseLockedException {
        var abonnement = abonnementService.getActiveEntity(societeId);

        int quota = 0;
        boolean hybride = false;
        if (abonnement != null) {
            AbonnementPlan plan = abonnementPlanRepository.findById(abonnement.getPlanId())
                    .orElseThrow(() -> new RuntimeException("AbonnementPlan not found: " + abonnement.getPlanId()));
            quota = plan.getEntrepriseQuota();
            hybride = abonnement.isHybride();
        }

        long count = repository.countBySociete_Id(societeId);

        if (quota == 1 && !hybride && count >= 1) {
            throw new EntrepriseLockedException(
                    "La société " + societeId + " est verrouillée sur son unique entreprise (plan sans hybride)");
        }
        if (count >= quota) {
            throw new EntrepriseQuotaExceededException(
                    "Quota d'entreprises atteint (" + quota + ") pour la société " + societeId);
        }

        var dto_ = create(dto);
        var entity = repository.findById(dto_.getId())
                .orElseThrow(() -> new RuntimeException("Entreprise not found: " + dto_.getId()));
        entity.setSociete(Societe.builder().id(societeId).build());
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    /**
     * A société on a 1-entreprise plan without hybride stays on its first entreprise for good:
     * it can neither delete it (then register another) nor change its identity (name / code).
     * Without any active abonnement the plan is unknown, so the entreprise is locked as well.
     */
    private boolean isLockedOnItsEntreprise(Entreprise entreprise) {
        if (entreprise.getSociete() == null) return false;
        var abonnement = abonnementService.getActiveEntity(entreprise.getSociete().getId());
        if (abonnement == null) return true;
        var plan = abonnementPlanRepository.findById(abonnement.getPlanId())
                .orElseThrow(() -> new RuntimeException("AbonnementPlan not found: " + abonnement.getPlanId()));
        return plan.getEntrepriseQuota() == 1 && !abonnement.isHybride();
    }

    @Override
    public EntrepriseDto update(EntrepriseDto dto) {
        var entity_database = repository.findById(dto.getId()).get();
        if (dto.getSubstitute() != null && entity_database.getSubstitute() != null
                && identityChanged(entity_database.getSubstitute(), dto.getSubstitute())
                && isLockedOnItsEntreprise(entity_database)) {
            throw new EntrepriseLockedException(
                    "Le nom et le code de l'entreprise ne peuvent pas être modifiés : plan sans option hybride");
        }
        var entity = mapper.asEntity(dto);

        entity.setSubstitute(dto.getSubstitute());
        entity.setCreateAt(entity_database.getCreateAt());
        entity.setUpdatedAt(entity_database.getUpdatedAt());
        entity.setSociete(entity_database.getSociete());
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

    private static boolean identityChanged(EntrepriseDatasSubstitute current, EntrepriseDatasSubstitute requested) {
        var sameName = current.getNameEntreprise() == null
                ? requested.getNameEntreprise() == null
                : current.getNameEntreprise().equalsIgnoreCase(requested.getNameEntreprise());
        return !sameName || !Objects.equals(current.getCode_importeur(), requested.getCode_importeur());
    }

    @Override
    public void delete(Long id) {
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entreprise not found"));
        if (isLockedOnItsEntreprise(entity)) {
            throw new EntrepriseLockedException(
                    "L'entreprise ne peut pas être supprimée : plan sans option hybride");
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
    public EntrepriseDto getName(String name, Long societeId) {
        var entity = repository.findBySociete_IdAndSubstitute_NameEntrepriseEqualsIgnoreCase(societeId, name);
        return entity == null ? null : mapper.asDto(entity);
    }

    @Override
    public EntrepriseDto getByRegistreCommerce(String registreCommerce) {
        if (registreCommerce == null || registreCommerce.isBlank()) return null;
        var found = repository.findByRegistreCommerce(registreCommerce.trim());
        return found.isEmpty() ? null : mapper.asDto(found.get(0));
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
    public Page<EntrepriseDto> getAllBySociete(Long societeId, Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(QEntreprise.entreprise.societe.id.eq(societeId));
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }



    @Override
    public EntrepriseExportDto export(Long entrepriseId) {
        var entity = repository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise not found: " + entrepriseId));
        var entrepriseDto = mapper.asDto(entity);
        return new EntrepriseExportDto(
                entrepriseDto,
                dirigeantEntrepriseService.getListByEntreprise(entrepriseId),
                banqueEntrepriseService.getListByEntreprise(entrepriseId),
                actionaireEntrepriseService.getListByEntreprise(entrepriseId),
                activiteEntrepriseService.getListByEntreprise(entrepriseId),
                gerantEntrepriseService.getListByEntreprise(entrepriseId)
        );
    }

    @Override
    public EntrepriseDto wipeExceptIdentity(Long entrepriseId) {
        var entity = repository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise not found: " + entrepriseId));

        // Defensive null-checks: EntrepriseMapperImpl builds new entities via the Lombok
        // builder (not the no-arg constructor), which does not run the collections' field
        // initializers, so an Entreprise that still is the very object created by create()
        // earlier in the same persistence context can have null collections here instead of
        // Hibernate-managed empty ones.
        if (entity.getActiviteEntreprises() != null) entity.getActiviteEntreprises().clear();
        if (entity.getActionaireEntreprises() != null) entity.getActionaireEntreprises().clear();
        if (entity.getDirigentEntreprises() != null) entity.getDirigentEntreprises().clear();
        if (entity.getGerantEntreprises() != null) entity.getGerantEntreprises().clear();
        if (entity.getBanqueEntreprises() != null) entity.getBanqueEntreprises().clear();

        var original = entity.getSubstitute();
        var wiped = new EntrepriseDatasSubstitute();
        if (original != null) {
            wiped.setNameEntreprise(original.getNameEntreprise());
            wiped.setCode_importeur(original.getCode_importeur());
        }
        entity.setSubstitute(wiped);

        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    private void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
        if (Objects.nonNull(searchParams)) {
            var qEntity = QEntreprise.entreprise;

            if (searchParams.containsKey("name"))
                booleanBuilder.and(qEntity.substitute.nameEntreprise.containsIgnoreCase(searchParams.get("name")));
        }
    }

}
