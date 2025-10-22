package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("entreprises")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EntrepriseRestController {
    private final EntrepriseService service;
    private final EntrepriseMapper mapper;


    @Operation(summary = "Creer une entreprise", description = "Cet uri prend une entreprise et la sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createEntreprise(@RequestBody EntrepriseDto modelDto) {
        try {
            var isExisteEntreprise = service.getName(modelDto.getName());
            if (isExisteEntreprise!=null){
                return Response.duplicateReference().setMessage("L'entreprise "+modelDto.getName()+" existe deja");
            }
            var dto = service.create(modelDto);
            return Response.ok().setPayload(dto).setMessage("Entreprise créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier une entreprise", description = "Cet uri prend une entreprise et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateEntreprise(@Parameter(name = "id", description = "l'id de l'entreprise a mettre a jour") @PathVariable("id") Long id, @RequestBody EntrepriseDto modelDto) {
        modelDto.setId(id);
        try {
            var isExisteEntreprise = service.getName(modelDto.getName());
            if (isExisteEntreprise!=null && isExisteEntreprise.getId()!=id){
                return Response.duplicateReference().setMessage("L'entreprise "+modelDto.getName()+" existe deja");
            }
            var dto = service.update(modelDto);
            return Response.ok().setPayload(dto).setMessage("Entreprise modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer une entreprise", description = "Cet uri permet de recuperer une entreprise, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getEntreprise(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Entreprise trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister toutes les entreprises", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEntreprises(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Supprimer une entreprise", description = "Cet uri supprimer l'entreprise correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntreprise(@PathVariable("id") Long id) {
        try {
            EntrepriseDto dto = service.get(id);
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
