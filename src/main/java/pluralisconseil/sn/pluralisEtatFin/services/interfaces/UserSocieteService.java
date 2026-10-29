package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.security.core.Authentication;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.SocieteMemberDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.exceptions.InsufficientRoleException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;

import java.util.List;

/**
 * Resolves and manages the membership link between a Keycloak user and the {@code Societe}
 * they belong to.
 *
 * <p>{@link #resolveCurrent(Authentication)} is the single shared hook every controller added
 * or modified from Task 3 onward must call to get the caller's société/role, instead of
 * re-extracting the JWT {@code sub} inline.
 */
public interface UserSocieteService {

    /**
     * Links a Keycloak user to a société with the given role. No hierarchy/permission check is
     * performed here — this is a trusted internal call, made only from code that has already
     * decided the attachment is legitimate (e.g. {@link SocieteService#create}).
     */
    UserSociete attach(String keycloakUserId, Long societeId, String role);

    UserSocieteDto resolveByKeycloakUserId(String keycloakUserId) throws SocieteMembershipNotFoundException;

    /**
     * Extracts the caller's Keycloak {@code sub} from the JWT the same way
     * {@code MeRestController} does, then resolves their société membership.
     */
    UserSocieteDto resolveCurrent(Authentication authentication) throws SocieteMembershipNotFoundException;

    /** The members of a société with their names (from Keycloak) and role; accounts gone from Keycloak are left out. */
    List<SocieteMemberDto> listMembers(Long societeId);

    /**
     * Creates a new Keycloak-backed collaborator and attaches them to the caller's own société,
     * enforcing the role hierarchy: only a caller whose own role is {@code ADMIN_COMPTE} may call
     * this method, and they may only grant {@code COLLABORATEUR} or {@code INVITE} (never
     * {@code ADMIN_COMPTE} or {@code ADMIN_PLATEFORME} — platform/société admins are created via
     * {@code SocieteService#create}).
     *
     * @param callerKeycloakUserId the Keycloak {@code sub} of the caller (an {@code ADMIN_COMPTE})
     * @param requestedRole        the {@code plural_etatfi} client role to grant the new user
     * @throws InsufficientRoleException if the caller is not an {@code ADMIN_COMPTE}, or if
     *                                    {@code requestedRole} is {@code ADMIN_COMPTE} or
     *                                    {@code ADMIN_PLATEFORME}
     */
    UserSocieteDto createMember(String callerKeycloakUserId, String username, String email,
                                 String firstName, String lastName, String temporaryPassword,
                                 String requestedRole) throws InsufficientRoleException;
}
