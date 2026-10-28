package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getPlan(@PathVariable Long id) {
        try {
            return Response.ok().setPayload(service.get(id));
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllPlans() {
        try {
            return Response.ok().setPayload(service.getAll());
        } catch (Exception ex) {
            return Response.exception().setMessage("Une erreur est survenue");
        }
    }

    public record CreateAbonnementPlanRequest(
            String nom, int entrepriseQuota, boolean hybrideInclus, boolean hybrideDisponibleEnOption,
            BigDecimal hybrideSupplementPrix, BigDecimal prixParMois, boolean active) {
    }
}
