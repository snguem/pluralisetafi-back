package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.models.IdentifiableDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;

import java.util.Map;

/**
 * Shared REST surface for every "X of an Entreprise" controller (dirigeants, banques,
 * actionnaires, activites, gerants) — previously duplicated almost byte-for-byte across all
 * five, down to the unused mapper field each one injected but never called.
 */
public abstract class AbstractEntrepriseChildController<D extends IdentifiableDto> {

    protected abstract EntrepriseChildService<D> service();

    /** e.g. "Dirigeant", "Banque" — used only in user-facing messages. */
    protected abstract String entityLabel();

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> create(@RequestBody D dto) {
        try {
            var saved = service().create(dto);
            return Response.ok().setPayload(saved).setMessage(entityLabel() + " créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> update(@PathVariable("id") Long id, @RequestBody D dto) {
        dto.setId(id);
        try {
            var saved = service().update(dto);
            return Response.ok().setPayload(saved).setMessage(entityLabel() + " modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> get(@PathVariable Long id) {
        try {
            var dto = service().get(id);
            return Response.ok().setPayload(dto).setMessage(entityLabel() + " trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAll(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service().getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @GetMapping("/all-entreprise")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEntreprise(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable,
            @RequestParam long entreprise_id) {
        var page = service().getAllByEntreprise(entreprise_id, searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        try {
            service().delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
