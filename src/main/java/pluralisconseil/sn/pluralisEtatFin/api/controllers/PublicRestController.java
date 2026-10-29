package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DashboardService;

/** Open to everyone, no sign-in: what the landing page shows. Nothing société-specific here. */
@RestController
@RequestMapping("public")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PublicRestController {

    private final DashboardService service;

    @GetMapping("/plans")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> plans() {
        return Response.ok().setPayload(service.publicPlans());
    }
}
