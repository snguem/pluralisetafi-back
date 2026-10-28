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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierFormDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.exceptions.FileNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.NotFoundException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelCleanupService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelTemplateService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelToPdfConverter;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierImportService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("etats-fin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EtatFinancierRestController {
    private final EtatFinancierService service;
    private final ModelExcelService modelExcelService;
    private final HelperService helperService;
    private final ExcelTemplateService excelTemplateService;
    private final ExcelCleanupService excelCleanupService;
    private final ExcelToPdfConverter excelToPdfConverter;
    private final EtatFinancierImportService importService;
    private final UserSocieteService userSocieteService;
    private final EntrepriseService entrepriseService;

    /**
     * Resolves the caller's société via {@link UserSocieteService#resolveCurrent(Authentication)}
     * and verifies that the {@code EtatFinancier} identified by {@code etatFinancierId} belongs
     * to that société (via its real {@code entrepriseRef.societe.id} relation, never the
     * possibly-stale embedded {@code entreprise} snapshot). Throws {@link UnauthorizeException}
     * on any mismatch or missing membership, mapped to 401 by {@code ExceptionInterceptor}'s
     * {@code @ControllerAdvice} wherever it is allowed to propagate, or caught locally and mapped
     * to {@link Response#unauthorized()} / an HTTP 401 {@code ResponseEntity} otherwise. Called at
     * the top of every id-based endpoint in this controller so a {@code COLLABORATEUR} of another
     * société cannot read, modify, delete, or export (Excel/PDF) a given état financier just by
     * guessing its id.
     */
    private void requireOwnership(Long etatFinancierId, Authentication authentication) {
        UserSocieteDto caller;
        try {
            caller = userSocieteService.resolveCurrent(authentication);
        } catch (SocieteMembershipNotFoundException ex) {
            throw new UnauthorizeException(ex.getMessage());
        }
        var entity = service.getEntity(etatFinancierId);
        if (entity.getEntrepriseRef() == null || entity.getEntrepriseRef().getSociete() == null
                || !entity.getEntrepriseRef().getSociete().getId().equals(caller.getSocieteId())) {
            throw new UnauthorizeException("Accès refusé à cet état financier");
        }
    }

    /**
     * The failure messages produced internally by {@code EtatFinancierImportServiceImpl} for its
     * "merge unavailable" / "configuration failed" cases. The type that signals the first of
     * these ({@code BalanceMergeUnavailableException}) is private and nested inside that service
     * implementation, so it is not reachable from this controller's package; matching on the
     * exception message is the only way to preserve the original {@code Response.exception()}
     * mapping for these specific cases without modifying the already-reviewed service.
     */
    private static final Set<String> KNOWN_CONFIG_OR_MERGE_FAILURE_MESSAGES = Set.of(
            "Impossible de fusionner les balances",
            "Une erreur s'est produite lors de la configuration des fichiers",
            "Une erreur s'est produitehgd lors de la configuration des fichiers"
    );

    private static boolean isKnownConfigOrMergeFailureMessage(String message) {
        return message != null && KNOWN_CONFIG_OR_MERGE_FAILURE_MESSAGES.contains(message);
    }

    @Operation(summary = "Creer un etatFinancier", description = "Cet uri prend un etatFinancier et le sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createEtatFinancier(@RequestBody EtatFinancierFormDto etatFinancierFormDto) {
        try {
            var model = modelExcelService.get(etatFinancierFormDto.getModel_id());
            var etat_fin = etatFinancierFormDto.getEtat_financier();
            var entreprise= etatFinancierFormDto.getEntreprise();
//            entreprise infos set
            etat_fin.setEntreprise(entreprise);

//            model excel set
            etat_fin.setModel_excel_path(model.getExcelPath());
            etat_fin.setModelExcel_name(model.getName());
            etat_fin.setModelExcel_id(etatFinancierFormDto.getModel_id());
//
            etat_fin.setName("Etat Financier "+entreprise.getNameEntreprise()+ " "+ etatFinancierFormDto.getEtat_financier().getAnnee_n());
            etat_fin.setAnnee_n_1(etatFinancierFormDto.getEtat_financier().getAnnee_n_1());
            etat_fin.setAnnee_n(etatFinancierFormDto.getEtat_financier().getAnnee_n());
            var dto = service.create(etat_fin);
            etatFinancierFormDto.setId(dto.getId());
            etatFinancierFormDto.setEtat_financier(dto);
            return Response.ok().setPayload(etatFinancierFormDto).setMessage("Etat Financier créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage("Une erreur est survenu lors de la creation");
        }
    }

    @Operation(summary = "Creer les fichier excels balance n et n-1", description = "Cet uri prend l'id d'un etat financier, les annees et balances N et N-1")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PutMapping(value = "/{id}/annees-balances",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> setBalances(
            Authentication authentication,
            @Parameter(name = "id", description = "l'id de l'etat financier a mettre a jour") @PathVariable("id") Long id,
            @RequestPart("balance_n") MultipartFile balance,
            @RequestPart("balance_n_1") MultipartFile balance_n
    ) {
        try {
            requireOwnership(id, authentication);
            var dto_updated = importService.importBalances(id, balance, balance_n);
            return Response.ok().setPayload(dto_updated).setMessage("Balances fusionne");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (FileNotFoundException f) {
            return Response.notFound().setMessage(f.getMessage());
        } catch (Exception ex) {
            if (isKnownConfigOrMergeFailureMessage(ex.getMessage())) {
                return Response.exception().setMessage(ex.getMessage());
            }
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Creer les fichier excels balance n et n-1 a partir d'un seul fichier", description = "Cet uri prend l'id d'un etat financier et un fichier de balances de 2 feuilles : la balance N puis la balance N-1")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PutMapping(value = "/{id}/annees-balances-n",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> setBalanceN(
            Authentication authentication,
            @Parameter(name = "id", description = "l'id de l'etat financier a mettre a jour") @PathVariable("id") Long id,
            @RequestPart("balance_n") MultipartFile balance
    ) {
        try {
            requireOwnership(id, authentication);
            var dto_updated = importService.importBalanceN(id, balance);
            return Response.ok().setPayload(dto_updated).setMessage("Balance fusionne");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (FileNotFoundException f) {
            return Response.notFound().setMessage(f.getMessage());
        } catch (Exception ex) {
            if (isKnownConfigOrMergeFailureMessage(ex.getMessage())) {
                return Response.exception().setMessage(ex.getMessage());
            }
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }


    @Operation(summary = "Correction des valeurs excel")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PutMapping(value = "/{id}/correct",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> correction(
            Authentication authentication,
            @Parameter(name = "id") @PathVariable("id") Long id,
            @RequestPart("correct") MultipartFile correct
    ) {
        try {
            requireOwnership(id, authentication);
            var dto=service.get(id);

            String correc = helperService.saveFile(0, "correct", correct);

            dto.setExcelPath(correc);

            service.update(dto);

            return Response.ok().setPayload(dto).setMessage("Etat financier modifier");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (FileNotFoundException f) {
            return Response.notFound().setMessage(f.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier un etat financier", description = "Cet uri prend un etat financier et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateEtatFinancier(Authentication authentication, @Parameter(name = "id", description = "l'id de l'etat financier a mettre a jour") @PathVariable("id") Long id, @RequestBody EtatFinancierDto modelDto) {
        modelDto.setId(id);
        try {
            requireOwnership(id, authentication);
            var dto = service.update(modelDto);
            return Response.ok().setPayload(dto).setMessage("Etat Financier modifié");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer un etat financier", description = "Cet uri permet de recuperer un etat financier, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getEtatFinancier(Authentication authentication, @Parameter @PathVariable Long id) {
        try {
            requireOwnership(id, authentication);
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Etat Financier trouvé");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }


    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "404", description = "Resource access does not exist"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @GetMapping("/{id}/excel")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<byte[]> downloadFile(Authentication authentication, @Parameter @PathVariable Long id) {
        try{
            requireOwnership(id, authentication);
            var dto=service.get(id);
            if (!helperService.isExist(dto.getExcelPath())) throw new NotFoundException("Fichier inexistant!");

            excelCleanupService.nettoyerEtRecupererExcel(dto.getExcelPath());

            File file = new File(dto.getExcelPath());

            Path path = Paths.get(file.getAbsolutePath());
            byte[] data = Files.readAllBytes(path);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dto.getName().replace(" ", "_") + ".xlsx");
            headers.setContentType(
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
            );
            headers.setContentLength(data.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (UnauthorizeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }catch (NotFoundException | IOException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    //    @Operation(summary = "Liste des controle", description = "Cet uri permet de recuperer un controle, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "404", description = "Resource access does not exist"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @GetMapping("/{id}/page-names")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getPageNames(Authentication authentication, @Parameter @PathVariable Long id) {
        try {
            requireOwnership(id, authentication);
            var dto = service.get(id);
            var names=excelTemplateService.getExcelPageNames(dto.getExcelPath());

            return Response.ok().setPayload(
                    names.stream().filter( nm -> !nm.toLowerCase().contains("evaluation warning")).collect(Collectors.toList())
            ).setMessage("Pages de l'etat financiers");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer le pdf du fichier excel selon une liste de pages", description = "Cet uri prend l'id de l'etat financier, les pages et retourne un pdf")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @PostMapping(value = "/{id}/pdf")
    public ResponseEntity<byte[]> exportExcelToPdfA4Marging(Authentication authentication, @PathVariable("id") long id, @RequestBody List<String> pagesNames) {
        try {
            requireOwnership(id, authentication);
            var dto=service.get(id);
            byte[] pdf=excelToPdfConverter.toPdf(dto.getExcelPath(), pagesNames);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (UnauthorizeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur conversion: " + e.getMessage()).getBytes());
        }
    }


    @Operation(summary = "Lister tout les etats financier", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEtatFinanciers(Authentication authentication, @RequestParam Map<String, String> searchParams, Pageable pageable) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var page = service.getAllBySociete(caller.getSocieteId(), searchParams, pageable);
            Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
            return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister tout les etats financier d'une entreprise", description = "Cet uri prend l'id d'une entreprise, des parametres de filtre et de page et retourn la liste des etats financiers correspondant")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all-entreprise")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEtatFinanciersEntreprise(
                                                            Authentication authentication,
                                                            @RequestParam Map<String, String> searchParams,
                                                           Pageable pageable,
                                                            @RequestParam long entreprise_id){
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var entreprise = entrepriseService.getEntity(entreprise_id);
            if (entreprise.getSociete() == null || !entreprise.getSociete().getId().equals(caller.getSocieteId())) {
                return Response.unauthorized().setMessage("Accès refusé à cette entreprise");
            }
            var page = service.getAllByEntreprise(entreprise_id, searchParams, pageable);
            Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
            return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Supprimer un etat financier", description = "Cet uri supprimer l'etat financier correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEtatFinancier(Authentication authentication, @PathVariable("id") Long id) {
        try {
            requireOwnership(id, authentication);
            EtatFinancierDto dto = service.get(id);
            helperService.remove(dto.getExcelPath());
            service.delete(id);
        } catch (UnauthorizeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Supprimer tous les etats financiers d'une societe", description = "Cet uri supprime tous les etats financiers dont l'entreprise appartient a la societe de l'appelant; reserve a l'ADMIN_COMPTE de cette societe")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "401"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/societe")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> deleteAllBySociete(Authentication authentication) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            if (!"ADMIN_COMPTE".equals(caller.getRole())) {
                return Response.unauthorized().setMessage("Accès refusé");
            }
            service.deleteAllBySociete(caller.getSocieteId());
            return Response.ok().setMessage("Etats financiers supprimés");
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }
}
