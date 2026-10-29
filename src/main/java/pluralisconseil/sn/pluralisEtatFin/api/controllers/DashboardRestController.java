package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DashboardService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

/**
 * Figures for the frontend shell and dashboards. {@code /me/societe} and {@code /stats/societe}
 * are for members of a société (any role, read-only société included); {@code /admin/stats} is
 * for the platform admin (already gated by {@code SecurityConfig}'s {@code /admin/**} rule).
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin("*")
public class DashboardRestController {

    private final DashboardService service;
    private final UserSocieteService userSocieteService;

    @GetMapping("/me/societe")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> meSociete(Authentication authentication) {
        try {
            return Response.ok().setPayload(service.meSociete(userSocieteService.resolveCurrent(authentication)));
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/stats/societe")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> societeStats(Authentication authentication) {
        try {
            return Response.ok().setPayload(service.societeStats(userSocieteService.resolveCurrent(authentication)));
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/admin/societes/overview")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> societesOverview() {
        return Response.ok().setPayload(service.societesOverview());
    }

    @GetMapping("/admin/stats")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> platformStats() {
        return Response.ok().setPayload(service.platformStats());
    }
}
