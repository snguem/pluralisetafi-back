package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSocieteServiceImpl implements UserSocieteService {

    private final UserSocieteRepository repository;
    private final UserSocieteMapper mapper;

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
        return mapper.asDto(entity);
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
}
