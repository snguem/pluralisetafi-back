package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.CreateSocieteUserRequest;
import pluralisconseil.sn.pluralisEtatFin.exceptions.InsufficientRoleException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

/**
 * Lets an {@code ADMIN_COMPTE} manage the membership of their own société: create new
 * Keycloak-backed collaborators within the role hierarchy, and list the société's current
 * members. Guarded at the HTTP layer by {@code SecurityConfig}'s
 * {@code /societe/** -> hasRole(ADMIN_COMPTE)} rule; the caller's société/role is always resolved
 * via {@link UserSocieteService#resolveCurrent(Authentication)}, never by re-extracting the JWT
 * {@code sub} here.
 */
@Slf4j
@RestController
@RequestMapping("societe/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SocieteUserRestController {

    private final UserSocieteService userSocieteService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> createUser(Authentication authentication, @RequestBody CreateSocieteUserRequest request) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            UserSocieteDto created = userSocieteService.createMember(
                    caller.getKeycloakUserId(), request.username(), request.email(), request.firstName(),
                    request.lastName(), request.temporaryPassword(), request.role());
            return Response.created().setPayload(created);
        } catch (InsufficientRoleException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (KeycloakUserAlreadyExistsException ex) {
            return Response.duplicateEmail().setMessage(ex.getMessage());
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.notFound().setMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "SocieteUserRestController", ex);
            return Response.exception().setMessage("Une erreur est survenue, consultez les logs du serveur");
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> listUsers(Authentication authentication) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            return Response.ok().setPayload(userSocieteService.listMembers(caller.getSocieteId()));
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.notFound().setMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "SocieteUserRestController", ex);
            return Response.exception().setMessage("Une erreur est survenue, consultez les logs du serveur");
        }
    }
}
