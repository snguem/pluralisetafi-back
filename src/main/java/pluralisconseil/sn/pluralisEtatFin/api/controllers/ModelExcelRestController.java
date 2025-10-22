package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ModelExcelMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelService;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import java.util.Map;

@RestController
@RequestMapping("models")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ModelExcelRestController {
    private final ModelExcelService service;
    private final ExcelService excelService;
    private final HelperService helperService;
    private final ModelExcelMapper mapper;


    @Operation(summary = "Creer un model", description = "Cet uri prend un model et le sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createModelExcel(@RequestPart("excel") MultipartFile excel,
                                             @RequestPart String name) {
        try {
            String path = helperService.saveFile(0, "modele", excel);
            if (path!=null){
                var isExisteEntreprise = service.getName(name);
                if (isExisteEntreprise!=null){
                    return Response.duplicateReference().setMessage("Le modele `"+name+"` existe deja");
                }
                var model = new ModelExcelDto();
                model.setName(name);
                model.setExcelPath(path);
                model.setActive(true);
                var dto = service.create(model);
                return Response.ok().setPayload(dto).setMessage("Model excel cree");
            }else {
                return Response.exception().setMessage("Une erreur est survenu lors de l'enregistrement du fichier");
            }
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier un model", description = "Cet uri prend un model et modifi sa reference dans la base de donnees")
    @PutMapping(value="/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateModelExcel(@Parameter(name = "id", description = "l'id du model a mettre a jour") @PathVariable("id") Long id,
                                             @RequestPart("excel") MultipartFile excel,
                                             @RequestPart String name) {
        try {
            var isExisteEntreprise = service.getName(name);
            if (isExisteEntreprise!=null && isExisteEntreprise.getId()!=id){
                return Response.duplicateReference().setMessage("Le modele `"+name+"` existe deja");
            }
            var dto_entity=service.get(id);
            helperService.remove(dto_entity.getExcelPath());
            String path = helperService.saveFile(0, "modele", excel);
            if (path!=null){
                var model = new ModelExcelDto();
                model.setName(name);
                model.setExcelPath(path);
                model.setActive(true);
                var dto = service.create(model);
                return Response.ok().setPayload(dto).setMessage("Model excel modifier");
            }else {
                return Response.exception().setMessage("Une erreur est survenu lors de l'enregistrement du fichier");
            }
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer un model", description = "Cet uri permet de recuperer un model, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getModelExcel(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Model excel trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    //    @Operation(summary = "Liste des controle", description = "Cet uri permet de recuperer un controle, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "404", description = "Resource access does not exist"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @GetMapping("/{id}/page-names")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getPageModelNames(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            var names=excelService.getExcelPageNames(dto.getExcelPath());
            return Response.ok().setPayload(names).setMessage("Pages du modele");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister tout les models", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllModelExcels(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Supprimer un model", description = "Cet uri supprimer l'model correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModelExcel(@PathVariable("id") Long id) {
        try {
            ModelExcelDto dto = service.get(id);
            helperService.remove(dto.getExcelPath());
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
