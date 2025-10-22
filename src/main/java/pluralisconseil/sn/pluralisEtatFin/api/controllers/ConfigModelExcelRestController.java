package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ConfigModelExcelMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ConfigModelExcelService;

import java.util.Map;

@RestController
@RequestMapping("config-model-excel")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ConfigModelExcelRestController {
    private final ConfigModelExcelService service;
    private final ConfigModelExcelMapper mapper;


    @Operation(summary = "Creer une config", description = "Cet uri prend une config et la sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createConfigModelExcel(@RequestBody ConfigModelExcelDto modelDto) {
        try {
            var dto = service.create(modelDto);
            return Response.ok().setPayload(dto).setMessage("Config créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier une config", description = "Cet uri prend une config et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateConfigModelExcel(@Parameter(name = "id", description = "l'id de la config a mettre a jour") @PathVariable("id") Long id, @RequestBody ConfigModelExcelDto modelDto) {
        modelDto.setId(id);
        try {
            var dto = service.update(modelDto);
            return Response.ok().setPayload(dto).setMessage("Config modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer une config", description = "Cet uri permet de recuperer une config, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getConfigModelExcel(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Config trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister toutes les configs", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllConfigModelExcels(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }
    
    @Operation(summary = "Lister toutes les configs d'un model", description = "Cet uri prend l'id d'un model excel, des parametres de filtre et de page et retourn la liste des configs correspondant")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all-model")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllConfigModelExcelsEntreprise(
                                                            @RequestParam Map<String, String> searchParams,
                                                           Pageable pageable,
                                                            @RequestParam long model_id){
        var page = service.getAllByModelExcel(model_id, searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Supprimer une config", description = "Cet uri supprimer une config correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConfigModelExcel(@PathVariable("id") Long id) {
        try {
            ConfigModelExcelDto dto = service.get(id);
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
