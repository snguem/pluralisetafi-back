package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EtatFinancierMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierFormDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelService;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("etats-fin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EtatFinancierRestController {
    private final EtatFinancierService service;
    private final EntrepriseService entrepriseService;
    private final ModelExcelService modelExcelService;
    private final EtatFinancierMapper mapper;
    private final HelperService helperService;
    private final ExcelService excelService;

    @Operation(summary = "Creer un etatFinancier", description = "Cet uri prend un etatFinancier et le sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createEtatFinancier(@RequestBody EtatFinancierFormDto etatFinancierFormDto) {
        try {
            var etat_fin = etatFinancierFormDto.getEtat_financier();
            var entreprise= entrepriseService.getName(etatFinancierFormDto.getEntreprise().getName());
            etat_fin.setModelExcel_id(etatFinancierFormDto.getModel_id());
            etat_fin.setEntrepriseId(entreprise.getId());
            etat_fin.setName("Etat Financier "+entreprise.getName()+ " "+ etatFinancierFormDto.getEtat_financier().getAnnee_n());
            etat_fin.setAnnee_n_1(etatFinancierFormDto.getEtat_financier().getAnnee_n_1());
            etat_fin.setAnnee_n(etatFinancierFormDto.getEtat_financier().getAnnee_n());
            var dto = service.create(etat_fin);
            etatFinancierFormDto.setId(dto.getId());
            etatFinancierFormDto.setEtat_financier(dto);
            return Response.ok().setPayload(etatFinancierFormDto).setMessage("Etat Financier créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }
    @Operation(summary = "Creer les fichier excels balance n et n-1", description = "Cet uri l'id d'un etat financier, les annees et balances N et N-1")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PutMapping(value = "/{id}/annees-balances",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> updateBalance(
            @Parameter(name = "id", description = "l'id de l'etat financier a mettre a jour") @PathVariable("id") Long id,
            @RequestPart("balance_n") MultipartFile balance,
            @RequestPart("balance_n_1") MultipartFile balance_n
    ) {
        try {
            var dto=service.get(id);
            var model_=modelExcelService.get(dto.getModelExcel_id());
//            var entreprise_dto=entrepriseService.get(dto.getEntrepriseId());
            Entreprise entreprise_=entrepriseService.getEntity(dto.getEntrepriseId());

            String balance_n_temp = helperService.saveTempFile(balance);
            String balance_n_1_temp = helperService.saveTempFile(balance_n);
            EtatFinancierDto new_dto_with_fuse=null;
            if (balance_n_1_temp!=null && balance_n_temp!=null){
                new_dto_with_fuse = excelService.fuseBalanceToExcel(dto, model_.getExcelPath(), balance_n_temp, balance_n_1_temp);
            }else{
                new_dto_with_fuse = excelService.fuseBalanceToExcel(dto, model_.getExcelPath(), balance_n_temp);
            }

            var final_etat_fin=excelService.configExcelFile(new_dto_with_fuse, entreprise_);
            if (final_etat_fin!=null){
                var dto_updated = service.update(final_etat_fin);
                helperService.removesTemp(List.of(balance_n_temp, balance_n_1_temp));
                return Response.ok().setPayload(dto_updated).setMessage("Balance fusionne");
            }else {
                service.delete(id);
                helperService.removesTemp(List.of(balance_n_temp, balance_n_1_temp));
                return Response.exception().setMessage("Une erreur s'est produite lors de la configuration des fichiers");
            }
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier un etat financier", description = "Cet uri prend un etat financier et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateEtatFinancier(@Parameter(name = "id", description = "l'id de l'etat financier a mettre a jour") @PathVariable("id") Long id, @RequestBody EtatFinancierDto modelDto) {
        modelDto.setId(id);
        try {
            var dto = service.update(modelDto);
            return Response.ok().setPayload(dto).setMessage("Etat Financier modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer un etat financier", description = "Cet uri permet de recuperer un etat financier, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getEtatFinancier(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Etat Financier trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }


    //    @Operation(summary = "Liste des controle", description = "Cet uri permet de recuperer un controle, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "404", description = "Resource access does not exist"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @GetMapping("/{id}/page-names")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getPageNames(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            var names=excelService.getExcelPageNames(dto.getExcelPath());

            return Response.ok().setPayload(
                    names.stream().filter( nm -> !nm.toLowerCase().contains("evaluation warning")).collect(Collectors.toList())
            ).setMessage("Pages du de l'etats financiers");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer le pdf du fichier excel selon une liste de pages", description = "Cet uri prend l'id de l'etat financier, les pages et retourne un pdf")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @PostMapping(value = "/{id}/pdf")
    public ResponseEntity<byte[]> exportExcelToPdfA4Marging(@PathVariable("id") long id, @RequestBody List<String> pagesNames) {
        try {
            var dto=service.get(id);
            byte[] pdf=excelService.toPdf(dto.getExcelPath(), pagesNames);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur conversion: " + e.getMessage()).getBytes());
        }
    }


    @Operation(summary = "Lister tout les etats financier", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEtatFinanciers(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Lister tout les etats financier d'une entreprise", description = "Cet uri prend l'id d'une entreprise, des parametres de filtre et de page et retourn la liste des etats financiers correspondant")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all-entreprise")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEtatFinanciersEntreprise(
                                                            @RequestParam Map<String, String> searchParams,
                                                           Pageable pageable,
                                                            @RequestParam long entreprise_id){
        var page = service.getAllByEntreprise(entreprise_id, searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Supprimer un etat financier", description = "Cet uri supprimer l'etat financier correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEtatFinancier(@PathVariable("id") Long id) {
        try {
            EtatFinancierDto dto = service.get(id);
            helperService.remove(dto.getExcelPath());
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
