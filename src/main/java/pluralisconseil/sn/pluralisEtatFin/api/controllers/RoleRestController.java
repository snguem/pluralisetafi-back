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
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.RoleService;

import java.util.Map;

@RestController
@RequestMapping("roles")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RoleRestController {
    private final RoleService service;
    private final RoleMapper mapper;


    @Operation(summary = "Creer un role", description = "Cet uri prend un role et le sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createRole(@RequestBody RoleDto roleDto) {
        try {
            var dto = service.create(roleDto);
            return Response.ok().setPayload(dto).setMessage("Role créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier un role", description = "Cet uri prend un role et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateRole(@Parameter(name = "id", description = "l'id de l'role a mettre a jour") @PathVariable("id") Long id, @RequestBody RoleDto roleDto) {
        roleDto.setId(id);
        try {
            var dto = service.update(roleDto);
            return Response.ok().setPayload(dto).setMessage("Role modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer un role", description = "Cet uri permet de recuperer un role, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getRole(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("Role trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Lister tout les roles", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllRoles(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Lister tout les roles visible", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all-visible")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllRolesVisible() {
        var page = service.getAllVisible();
        return Response.ok().setPayload(page);
    }

    @Operation(summary = "Supprimer un role", description = "Cet uri supprimer l'role correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable("id") Long id) {
        try {
            RoleDto dto = service.get(id);
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
