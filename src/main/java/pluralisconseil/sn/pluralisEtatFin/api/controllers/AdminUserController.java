package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;

/**
 * Admin-only endpoint for creating collaborator accounts directly in Keycloak. There is no local
 * user table: {@link KeycloakUserService} talks to Keycloak via the {@code admin-service}
 * client's service account.
 */
@RestController
@RequestMapping("admin/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminUserController {

    private final KeycloakUserService keycloakUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> createUser(@RequestBody CreateUserRequest request) {
        try {
            String id = keycloakUserService.createCollaborator(
                    request.username(), request.email(), request.firstName(), request.lastName(),
                    request.temporaryPassword(), request.role());
            return Response.created().setPayload(id);
        } catch (KeycloakUserAlreadyExistsException ex) {
            return Response.duplicateEmail().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> listUsers() {
        try {
            return Response.ok().setPayload(keycloakUserService.listUsers());
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @GetMapping("/roles")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> listAvailableRoles() {
        try {
            return Response.ok().setPayload(keycloakUserService.listAvailableRoles());
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @PostMapping("/{targetId}/copy-access/{sourceId}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> copyAccess(@PathVariable String targetId, @PathVariable String sourceId) {
        try {
            keycloakUserService.copyAccess(sourceId, targetId);
            return Response.ok().setPayload(targetId);
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @PatchMapping("/{id}/block")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> blockUser(Authentication authentication, @PathVariable String id) {
        if (id.equals(authentication.getName())) {
            return Response.badRequest().setMessage("Vous ne pouvez pas bloquer votre propre compte");
        }
        return setEnabled(id, false, "Compte bloqué");
    }

    @PatchMapping("/{id}/unblock")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> unblockUser(@PathVariable String id) {
        return setEnabled(id, true, "Compte débloqué");
    }

    private Response<Object> setEnabled(String id, boolean enabled, String message) {
        try {
            keycloakUserService.setUserEnabled(id, enabled);
            return Response.ok().setPayload(id).setMessage(message);
        } catch (IllegalArgumentException ex) {
            return Response.notFound().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    public record CreateUserRequest(
            String username, String email, String firstName, String lastName, String temporaryPassword, String role) {
    }
}
