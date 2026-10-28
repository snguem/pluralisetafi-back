package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseLockedException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseQuotaExceededException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.Map;

/**
 * Every endpoint resolves the caller's société via
 * {@link UserSocieteService#resolveCurrent(Authentication)} (never by re-extracting the JWT
 * {@code sub} inline, per Task 1's note). {@code createEntreprise} enforces the société's
 * {@code Abonnement} quota/lock via {@link EntrepriseService#create(EntrepriseDto, Long)};
 * {@code getAllEntreprises} is scoped to the caller's société via
 * {@link EntrepriseService#getAllBySociete}; {@code getEntreprise}/{@code updateEntreprise}
 * reject with {@link Response#unauthorized()} when the fetched {@code Entreprise} belongs to a
 * different société than the caller's. {@code deleteEntreprise} keeps its existing {@code void}/
 * 204-on-success contract, so the same société-mismatch case instead throws
 * {@link UnauthorizeException} (mapped to 401 by {@code ExceptionInterceptor}'s
 * {@code @ControllerAdvice}), the same pattern already used by the other void-returning delete
 * endpoints in this codebase.
 */
@RestController
@RequestMapping("entreprises")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EntrepriseRestController {
    private final EntrepriseService service;
    private final EntrepriseMapper mapper;
    private final UserSocieteService userSocieteService;


    @Operation(summary = "Creer une entreprise", description = "Cet uri prend une entreprise et la sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createEntreprise(Authentication authentication, @RequestBody EntrepriseDto modelDto) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var isExisteEntreprise = service.getName(modelDto.getSubstitute().getNameEntreprise());
            if (isExisteEntreprise!=null){
                return Response.duplicateReference().setMessage("L'entreprise "+modelDto.getSubstitute().getNameEntreprise()+" existe deja");
            }
            var dto = service.create(modelDto, caller.getSocieteId());
            return Response.ok().setPayload(dto).setMessage("Entreprise créé");
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (EntrepriseQuotaExceededException | EntrepriseLockedException ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier une entreprise", description = "Cet uri prend une entreprise et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateEntreprise(Authentication authentication, @Parameter(name = "id", description = "l'id de l'entreprise a mettre a jour") @PathVariable("id") Long id, @RequestBody EntrepriseDto modelDto) {
        modelDto.setId(id);
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var existingEntity = service.getEntity(id);
            if (existingEntity.getSociete() == null || !existingEntity.getSociete().getId().equals(caller.getSocieteId())) {
                return Response.unauthorized().setMessage("Accès refusé à cette entreprise");
            }
            var isExisteEntreprise = service.getName(modelDto.getSubstitute().getNameEntreprise());
            if (isExisteEntreprise!=null && isExisteEntreprise.getId()!=id){
                return Response.duplicateReference().setMessage("L'entreprise "+modelDto.getSubstitute().getNameEntreprise()+" existe deja");
            }
            var dto = service.update(modelDto);
            return Response.ok().setPayload(dto).setMessage("Entreprise modifié");
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer une entreprise", description = "Cet uri permet de recuperer une entreprise, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getEntreprise(Authentication authentication, @Parameter @PathVariable Long id) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var existingEntity = service.getEntity(id);
            if (existingEntity.getSociete() == null || !existingEntity.getSociete().getId().equals(caller.getSocieteId())) {
                return Response.unauthorized().setMessage("Accès refusé à cette entreprise");
            }
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Entreprise trouvé");
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister toutes les entreprises", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEntreprises(Authentication authentication, @RequestParam Map<String, String> searchParams, Pageable pageable) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var page = service.getAllBySociete(caller.getSocieteId(), searchParams, pageable);
            Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
            return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
        } catch (SocieteMembershipNotFoundException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Supprimer une entreprise", description = "Cet uri supprimer l'entreprise correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntreprise(Authentication authentication, @PathVariable("id") Long id) {
        try {
            UserSocieteDto caller = userSocieteService.resolveCurrent(authentication);
            var existingEntity = service.getEntity(id);
            if (existingEntity.getSociete() == null || !existingEntity.getSociete().getId().equals(caller.getSocieteId())) {
                throw new UnauthorizeException("Accès refusé à cette entreprise");
            }
            service.delete(id);
        } catch (SocieteMembershipNotFoundException ex) {
            throw new UnauthorizeException(ex.getMessage());
        } catch (UnauthorizeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
