package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.SocieteMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.SocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QSociete;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.SocieteService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SocieteServiceImpl implements SocieteService {

    private final SocieteRepository repository;
    private final SocieteMapper mapper;
    private final KeycloakUserService keycloakUserService;
    private final UserSocieteService userSocieteService;

    @Override
    public SocieteDto create(String nom, String code, String adminUsername, String adminEmail,
                              String adminFirstName, String adminLastName, String adminTemporaryPassword) {
        Societe entity = Societe.builder()
                .nom(nom)
                .code(code)
                .active(true)
                .build();
        Societe saved = repository.save(entity);

        String newUserId = keycloakUserService.createCollaborator(
                adminUsername, adminEmail, adminFirstName, adminLastName, adminTemporaryPassword, "ADMIN_COMPTE");
        userSocieteService.attach(newUserId, saved.getId(), "ADMIN_COMPTE");

        return mapper.asDto(saved);
    }

    @Override
    public SocieteDto get(Long id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Societe not found: " + id));
        return mapper.asDto(entity);
    }

    @Override
    public SocieteDto setActive(Long id, boolean active) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Societe not found: " + id));
        entity.setActive(active);
        return mapper.asDto(repository.save(entity));
    }

    @Override
    public Page<SocieteDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        buildSearch(searchParams, booleanBuilder);
        return repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
    }

    private void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
        if (Objects.nonNull(searchParams)) {
            var qEntity = QSociete.societe;

            if (searchParams.containsKey("nom"))
                booleanBuilder.and(qEntity.nom.containsIgnoreCase(searchParams.get("nom")));
            if (searchParams.containsKey("code"))
                booleanBuilder.and(qEntity.code.containsIgnoreCase(searchParams.get("code")));
        }
    }
}
