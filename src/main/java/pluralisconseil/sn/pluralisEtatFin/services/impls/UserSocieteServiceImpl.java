package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.SocieteMemberDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.InsufficientRoleException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserSocieteServiceImpl implements UserSocieteService {

    private static final String ADMIN_COMPTE = "ADMIN_COMPTE";
    private static final String ADMIN_PLATEFORME = "ADMIN_PLATEFORME";
    /** Header with which a platform admin chooses the société they work in. */
    public static final String SOCIETE_HEADER = "X-Societe-Id";
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

        Long chosenSociete = platformAdminChosenSociete(authentication);
        if (chosenSociete != null) {
            return resolvePlatformAdminIn(sub, chosenSociete);
        }
        return resolveByKeycloakUserId(sub);
    }

    /** The société a platform admin works in, from the {@link #SOCIETE_HEADER} header; null for anyone else. */
    private static Long platformAdminChosenSociete(Authentication authentication) {
        boolean platformAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + ADMIN_PLATEFORME).equals(authority.getAuthority()));
        if (!platformAdmin || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String header = attributes.getRequest().getHeader(SOCIETE_HEADER);
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException ex) {
            throw new SocieteMembershipNotFoundException("En-tête " + SOCIETE_HEADER + " invalide : " + header);
        }
    }

    /** A platform admin acts in the chosen société with the rights of its ADMIN_COMPTE. */
    private UserSocieteDto resolvePlatformAdminIn(String keycloakUserId, Long societeId) {
        var societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new SocieteMembershipNotFoundException("Société introuvable : " + societeId));
        if (Boolean.FALSE.equals(societe.getActive())) {
            throw new SocieteInactiveException("La société " + societeId + " est désactivée");
        }
        UserSocieteDto dto = new UserSocieteDto();
        dto.setKeycloakUserId(keycloakUserId);
        dto.setSocieteId(societeId);
        dto.setRole(ADMIN_COMPTE);
        dto.setReadOnly(!abonnementService.isCurrent(societeId));
        return dto;
    }

    @Override
    public List<SocieteMemberDto> listMembers(Long societeId) {
        return repository.findBySocieteId(societeId).stream()
                .map(member -> keycloakUserService.findUser(member.getKeycloakUserId())
                        .map(user -> new SocieteMemberDto(member.getId(), member.getKeycloakUserId(), user.username(),
                                user.email(), user.firstName(), user.lastName(), member.getRole(), user.enabled()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public UserSocieteDto createMember(String callerKeycloakUserId, String username, String email,
                                        String firstName, String lastName, String temporaryPassword,
                                        String requestedRole) throws InsufficientRoleException {
        return createMember(resolveByKeycloakUserId(callerKeycloakUserId), username, email, firstName, lastName,
                temporaryPassword, requestedRole);
    }

    @Override
    public UserSocieteDto createMember(UserSocieteDto caller, String username, String email,
                                        String firstName, String lastName, String temporaryPassword,
                                        String requestedRole) throws InsufficientRoleException {
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
