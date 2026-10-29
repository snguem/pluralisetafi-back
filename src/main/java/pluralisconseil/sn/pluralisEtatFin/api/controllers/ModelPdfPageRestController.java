package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelPdfPageDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelPdfPageService;

import java.util.List;
import java.util.Map;

/**
 * PDF print settings (area and orientation) of the sheets of an Excel model. Models are global, so
 * writing is reserved to the platform admin by the {@code /models/**} rules of SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("models/{modelId}/pdf-pages")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ModelPdfPageRestController {

    private final ModelPdfPageService service;

    @Operation(summary = "Réglages PDF des feuilles d'un modèle", description = "Liste, triée par feuille, ce que le modèle règle lui-même; les feuilles absentes suivent les règles par défaut")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAll(@Parameter @PathVariable Long modelId) {
        try {
            return Response.ok().setPayload(service.getAll(modelId));
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Feuilles du fichier Excel du modèle")
    @GetMapping("/sheets")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getSheets(@Parameter @PathVariable Long modelId) {
        try {
            return Response.ok().setPayload(service.getSheetNames(modelId));
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Ajouter le réglage d'une feuille")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> create(@PathVariable Long modelId, @RequestBody ModelPdfPageDto dto) {
        try {
            return Response.ok().setPayload(service.create(modelId, dto)).setMessage("Réglage créé");
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Modifier le réglage d'une feuille")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> update(@PathVariable Long modelId, @PathVariable Long id, @RequestBody ModelPdfPageDto dto) {
        try {
            return Response.ok().setPayload(service.update(modelId, id, dto)).setMessage("Réglage modifié");
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Supprimer le réglage d'une feuille", description = "La feuille suit alors les règles par défaut")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> delete(@PathVariable Long modelId, @PathVariable Long id) {
        try {
            service.delete(modelId, id);
            return Response.ok().setMessage("Réglage supprimé");
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Importer des réglages", description = "Prend une liste (même forme que l'export) : ajoute les feuilles absentes et met à jour celles qui existent")
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> importAll(@PathVariable Long modelId, @RequestBody List<ModelPdfPageDto> dtos) {
        try {
            var result = service.importAll(modelId, dtos);
            return Response.ok().setPayload(Map.of("created", result.created(), "updated", result.updated()))
                    .setMessage("Réglages importés");
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Partir des règles par défaut", description = "Crée le réglage des feuilles du fichier Excel qui n'en ont pas encore, avec les règles par défaut")
    @PostMapping("/defaults")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> seedDefaults(@PathVariable Long modelId) {
        try {
            return Response.ok().setPayload(service.seedDefaults(modelId)).setMessage("Réglages par défaut créés");
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    @Operation(summary = "Ajuster les feuilles à leur contenu", description = "Pour chaque feuille du fichier Excel qui contient quelque chose, règle la zone d'impression et l'orientation d'après son contenu réel; remplace les réglages existants")
    @PostMapping("/fit")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> fitToContent(@PathVariable Long modelId) {
        try {
            return Response.ok().setPayload(service.fitToContent(modelId)).setMessage("Feuilles ajustées à leur contenu");
        } catch (Exception ex) {
            return failure(ex);
        }
    }

    private static Response<Object> failure(Exception ex) {
        if (!(ex instanceof IllegalArgumentException)) {
            log.error("Echec de la requete ({})", "ModelPdfPageRestController", ex);
        }
        return Response.badRequest().setMessage(ex.getMessage());
    }
}
