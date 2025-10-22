package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.GerantEntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.GerantEntrepriseService;

import java.util.Map;

@RestController
@RequestMapping("gerants")
@RequiredArgsConstructor
@CrossOrigin("*")
public class GerantEntrepriseRestController {
    private final GerantEntrepriseService service;
    private final GerantEntrepriseMapper mapper;


    @Operation(summary = "Creer un gerant", description = "Cet uri prend un gerant et le sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createGerant(@RequestBody GerantEntrepriseDto modelDto) {
        try {
            var dto = service.create(modelDto);
            return Response.ok().setPayload(dto).setMessage("Gerant créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier un gerant", description = "Cet uri prend un gerant et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateGerant(@Parameter(name = "id", description = "l'id de du gerant a mettre a jour") @PathVariable("id") Long id, @RequestBody GerantEntrepriseDto modelDto) {
        modelDto.setId(id);
        try {
            var dto = service.update(modelDto);
            return Response.ok().setPayload(dto).setMessage("Gerant modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer un gerant", description = "Cet uri permet de recuperer un gerant, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getGerant(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Gerant trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister tout les etats financier", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllGerants(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }
    
    @Operation(summary = "Lister tout les etats financier d'une entreprise", description = "Cet uri prend l'id d'une entreprise, des parametres de filtre et de page et retourn la liste des etats financiers correspondant")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all-entreprise")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllGerantsEntreprise(
                                                            @RequestParam Map<String, String> searchParams,
                                                           Pageable pageable,
                                                            @RequestParam long entreprise_id){
        var page = service.getAllByEntreprise(entreprise_id, searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Supprimer un gerant", description = "Cet uri supprimer gerant correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGerant(@PathVariable("id") Long id) {
        try {
            GerantEntrepriseDto dto = service.get(id);
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
