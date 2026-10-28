package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.exceptions.KeycloakUserAlreadyExistsException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.SocieteService;

import java.util.Map;

/**
 * Admin-only endpoints for creating and listing sociétés, each created together with its first
 * admin account (provisioned directly in Keycloak via {@link SocieteService#create}).
 *
 * <p>Deliberately placed under the existing {@code /admin/**} prefix (gated by
 * {@code SecurityConfig}'s {@code hasRole("ADMIN_PLATEFORME")} rule) rather than a new
 * {@code /societe/**} prefix, so no new {@code SecurityConfig} rule is needed for this task. The
 * member-facing {@code /societe/**} prefix is deferred to a later task.
 */
@RestController
@RequestMapping("admin/societes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SocieteRestController {

    private final SocieteService service;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> createSociete(@RequestBody CreateSocieteRequest request) {
        try {
            var dto = service.create(
                    request.nom(), request.code(), request.adminUsername(), request.adminEmail(),
                    request.adminFirstName(), request.adminLastName(), request.adminTemporaryPassword());
            return Response.created().setPayload(dto);
        } catch (KeycloakUserAlreadyExistsException ex) {
            return Response.duplicateEmail().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getSociete(@PathVariable Long id) {
        try {
            return Response.ok().setPayload(service.get(id));
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllSocietes(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        try {
            var page = service.getAll(searchParams, pageable);
            Response.PageMetadata metadata = Response.PageMetadata.builder()
                    .number(page.getNumber())
                    .totalElements(page.getTotalElements())
                    .size(page.getSize())
                    .totalPages(page.getTotalPages())
                    .build();
            return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    public record CreateSocieteRequest(
            String nom, String code, String adminUsername, String adminEmail,
            String adminFirstName, String adminLastName, String adminTemporaryPassword) {
    }
}
