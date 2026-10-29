package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.InsufficientRoleException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserSocieteServiceImpl implements UserSocieteService {

    private static final String ADMIN_COMPTE = "ADMIN_COMPTE";
    private static final String ADMIN_PLATEFORME = "ADMIN_PLATEFORME";
    private static final Set<String> ROLES_AN_ADMIN_COMPTE_CANNOT_GRANT = Set.of(ADMIN_COMPTE, ADMIN_PLATEFORME);

    private final UserSocieteRepository repository;
    private final UserSocieteMapper mapper;
    private final KeycloakUserService keycloakUserService;
    private final SocieteRepository societeRepository;
    private final AbonnementService abonnementService;

    @Override
    public UserSociete attach(String keycloakUserId, Long societeId, String role) {
        UserSociete entity = UserSociete.builder()
                .keycloakUserId(keycloakUserId)
                .societeId(societeId)
                .role(role)
                .build();
        return repository.save(entity);
    }

    @Override
    public UserSocieteDto resolveByKeycloakUserId(String keycloakUserId) throws SocieteMembershipNotFoundException {
        UserSociete entity = repository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new SocieteMembershipNotFoundException(
                        "No société membership found for Keycloak user " + keycloakUserId));
        boolean deactivated = societeRepository.findById(entity.getSocieteId())
                .filter(societe -> Boolean.FALSE.equals(societe.getActive()))
                .isPresent();
        if (deactivated) {
            throw new SocieteInactiveException("La société " + entity.getSocieteId() + " est désactivée");
        }
        UserSocieteDto dto = mapper.asDto(entity);
        dto.setReadOnly(!abonnementService.isCurrent(entity.getSocieteId()));
        return dto;
    }

    @Override
    public UserSocieteDto resolveCurrent(Authentication authentication) throws SocieteMembershipNotFoundException {
        String sub = authentication.getName();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String subClaim = jwtAuthenticationToken.getToken().getClaimAsString("sub");
            if (subClaim != null) {
                sub = subClaim;
            }
        }

        return resolveByKeycloakUserId(sub);
    }

    @Override
    public List<UserSocieteDto> listMembers(Long societeId) {
        return mapper.parse(repository.findBySocieteId(societeId));
    }

    @Override
    public UserSocieteDto createMember(String callerKeycloakUserId, String username, String email,
                                        String firstName, String lastName, String temporaryPassword,
                                        String requestedRole) throws InsufficientRoleException {
        UserSocieteDto caller = resolveByKeycloakUserId(callerKeycloakUserId);

        if (!ADMIN_COMPTE.equals(caller.getRole())) {
            throw new InsufficientRoleException(
                    "Only an " + ADMIN_COMPTE + " can create société members; caller has role '"
                            + caller.getRole() + "'");
        }
        if (caller.isReadOnly()) {
            throw new InsufficientRoleException(
                    "L'abonnement de la société n'est pas en cours : impossible d'ajouter un membre");
        }
        if (ROLES_AN_ADMIN_COMPTE_CANNOT_GRANT.contains(requestedRole)) {
            throw new InsufficientRoleException(
                    "An " + ADMIN_COMPTE + " cannot create a member with role '" + requestedRole + "'");
        }

        String newUserId = keycloakUserService.createCollaborator(
                username, email, firstName, lastName, temporaryPassword, requestedRole);

        UserSociete entity = attach(newUserId, caller.getSocieteId(), requestedRole);
        return mapper.asDto(entity);
    }
}
