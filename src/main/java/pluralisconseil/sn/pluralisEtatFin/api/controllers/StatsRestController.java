package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.RoleMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.RoleDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.RoleService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("stats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class StatsRestController {
    private final EtatFinancierService etatFinancierService;
    private final EntrepriseService entrepriseService;
    private final UserService userService;


    @Operation(summary = "Recuperer le nombre d'entreprises, d'etats financiers et d'utilisateurs")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/entreprises-etats-fin-users")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getEntrepriseEtatFinUserStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("entreprise", entrepriseService.countAll());
            stats.put("document", etatFinancierService.countAll());
            stats.put("user", userService.countAll());
            return Response.ok().setPayload(stats).setMessage("Statistiques chargees");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

}
