package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.helpers.JsonService;
import pluralisconseil.sn.pluralisEtatFin.helpers.json.JsonMatriceModel;
import pluralisconseil.sn.pluralisEtatFin.helpers.json.JsonModel;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("json")
@RequiredArgsConstructor
@CrossOrigin("*")
public class JsonRestController {
    private final JsonService service;


    @PostMapping("/{sage}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> setJson(@PathVariable boolean sage, @RequestBody List<JsonModel> matrice) {
        try {
            return Response.ok().setPayload(service.setMatrice(matrice, sage)).setMessage("Matrice charge");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }


    @PutMapping("/{sage}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> addJson(@PathVariable boolean sage, @RequestBody JsonModel matrice) {
        try {
            return Response.ok().setPayload(service.addMatrice(matrice, sage)).setMessage("Matrice ajoutee");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }


    @GetMapping("/config")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getEntrepriseEtatFinUserStats(@RequestParam boolean sage) {
        try {
            return Response.ok().setPayload(service.getAllMatrice(sage)).setMessage("Matrice charge");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }


}
