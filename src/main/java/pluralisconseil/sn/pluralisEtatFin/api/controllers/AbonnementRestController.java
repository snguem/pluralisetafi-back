package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;

import java.time.LocalDate;

/**
 * Admin-only endpoints for activating and inspecting a {@code Societe}'s {@code Abonnement}.
 * Deliberately placed under the existing {@code /admin/**} prefix (gated by
 * {@code SecurityConfig}'s {@code hasRole("ADMIN_PLATEFORME")} rule), same reasoning as
 * {@code SocieteRestController}, so no new {@code SecurityConfig} rule is needed for this task.
 */
@Slf4j
@RestController
@RequestMapping("admin/abonnements")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AbonnementRestController {

    private final AbonnementService service;
    private final AbonnementMapper mapper;

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> activate(@RequestBody ActivateAbonnementRequest request) {
        try {
            var dto = service.activate(
                    request.societeId(), request.planId(), request.nbMois(), request.dateDebut(),
                    request.hybrideChoisi());
            return Response.created().setPayload(dto);
        } catch (IllegalArgumentException | SocieteInactiveException ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbonnementRestController", ex);
            return Response.exception().setMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @GetMapping("/societe/{societeId}/active")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getActive(@PathVariable Long societeId) {
        try {
            var entity = service.getActiveEntity(societeId);
            if (entity == null) {
                return Response.notFound();
            }
            return Response.ok().setPayload(mapper.asDto(entity));
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbonnementRestController", ex);
            return Response.exception().setMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    public record ActivateAbonnementRequest(
            Long societeId, Long planId, int nbMois, LocalDate dateDebut, boolean hybrideChoisi) {
    }
}
