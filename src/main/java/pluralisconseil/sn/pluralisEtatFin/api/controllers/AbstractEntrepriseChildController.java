package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseChildDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.IdentifiableDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.services.impls.SocieteAccessGuard;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Shared REST surface for every "X of an Entreprise" controller (dirigeants, banques,
 * actionnaires, activites, gerants) — previously duplicated almost byte-for-byte across all
 * five, down to the unused mapper field each one injected but never called.
 *
 * <p>Every operation is scoped to the caller's société: records are only reachable through an
 * entreprise that belongs to it.
 */
@Slf4j
public abstract class AbstractEntrepriseChildController<D extends IdentifiableDto & EntrepriseChildDto> {

    @Autowired
    private SocieteAccessGuard guard;

    protected abstract EntrepriseChildService<D> service();

    /** e.g. "Dirigeant", "Banque" — used only in user-facing messages. */
    protected abstract String entityLabel();

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> create(Authentication authentication, @RequestBody D dto) {
        try {
            var caller = guard.caller(authentication);
            SocieteAccessGuard.requireWrite(caller);
            guard.assertEntrepriseInSociete(caller, dto.getEntrepriseId());
            var saved = service().create(dto);
            return Response.ok().setPayload(saved).setMessage(entityLabel() + " créé");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbstractEntrepriseChildController", ex);
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> update(Authentication authentication, @PathVariable("id") Long id, @RequestBody D dto) {
        dto.setId(id);
        try {
            var caller = guard.caller(authentication);
            SocieteAccessGuard.requireWrite(caller);
            Long ownerId = service().getEntrepriseId(id);
            guard.assertEntrepriseInSociete(caller, ownerId);
            // a record can't be moved to another entreprise, so a société can't hand data to another one
            dto.setEntrepriseId(ownerId);
            var saved = service().update(dto);
            return Response.ok().setPayload(saved).setMessage(entityLabel() + " modifié");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbstractEntrepriseChildController", ex);
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> get(Authentication authentication, @PathVariable Long id) {
        try {
            var caller = guard.caller(authentication);
            guard.assertEntrepriseInSociete(caller, service().getEntrepriseId(id));
            var dto = service().get(id);
            return Response.ok().setPayload(dto).setMessage(entityLabel() + " trouvé");
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Echec de la requete ({})", "AbstractEntrepriseChildController", ex);
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAll(Authentication authentication, @RequestParam Map<String, String> searchParams, Pageable pageable) {
        try {
            var caller = guard.caller(authentication);
            var page = service().getAllBySociete(caller.getSocieteId(), pageable);
            Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
            return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        }
    }

    @GetMapping("/all-entreprise")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllEntreprise(
            Authentication authentication,
            @RequestParam Map<String, String> searchParams,
            Pageable pageable,
            @RequestParam long entreprise_id) {
        try {
            var caller = guard.caller(authentication);
            guard.assertEntrepriseInSociete(caller, entreprise_id);
            var page = service().getAllByEntreprise(entreprise_id, searchParams, pageable);
            Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
            return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
        } catch (UnauthorizeException ex) {
            return Response.unauthorized().setMessage(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable("id") Long id) {
        try {
            var caller = guard.caller(authentication);
            SocieteAccessGuard.requireWrite(caller);
            guard.assertEntrepriseInSociete(caller, service().getEntrepriseId(id));
            service().delete(id);
        } catch (UnauthorizeException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Echec de la requete ({})", "AbstractEntrepriseChildController", e);
            throw new RuntimeException(e);
        }
    }
}
