package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementPlanService;

import java.math.BigDecimal;

/**
 * Admin-only endpoints for managing the {@code AbonnementPlan} catalog. Deliberately placed
 * under the existing {@code /admin/**} prefix (gated by {@code SecurityConfig}'s
 * {@code hasRole("ADMIN_PLATEFORME")} rule), same reasoning as {@code SocieteRestController}, so
 * no new {@code SecurityConfig} rule is needed for this task.
 */
@Slf4j
@RestController
@RequestMapping("admin/abonnement-plans")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AbonnementPlanRestController {

    private final AbonnementPlanService service;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> createPlan(@RequestBody CreateAbonnementPlanRequest request) {
        try {
            var dto = service.create(
                    request.nom(), request.entrepriseQuota(), request.hybrideInclus(),
                    request.hybrideDisponibleEnOption(), request.hybrideSupplementPrix(),
                    request.prixParMois(), request.active());
            return Response.created().setPayload(dto);
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbonnementPlanRestController", ex);
            return Response.exception().setMessage("Une erreur est survenue, consultez les logs du serveur");
        }
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getPlan(@PathVariable Long id) {
        try {
            return Response.ok().setPayload(service.get(id));
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbonnementPlanRestController", ex);
            return Response.exception().setMessage("Une erreur est survenue, consultez les logs du serveur");
        }
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> deactivatePlan(@PathVariable Long id) {
        return setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> activatePlan(@PathVariable Long id) {
        return setActive(id, true);
    }

    private Response<Object> setActive(Long id, boolean active) {
        try {
            return Response.ok().setPayload(service.setActive(id, active));
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbonnementPlanRestController", ex);
            return Response.exception().setMessage("Une erreur est survenue, consultez les logs du serveur");
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllPlans() {
        try {
            return Response.ok().setPayload(service.getAll());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbonnementPlanRestController", ex);
            return Response.exception().setMessage("Une erreur est survenue, consultez les logs du serveur");
        }
    }

    public record CreateAbonnementPlanRequest(
            String nom, int entrepriseQuota, boolean hybrideInclus, boolean hybrideDisponibleEnOption,
            BigDecimal hybrideSupplementPrix, BigDecimal prixParMois, boolean active) {
    }
}
