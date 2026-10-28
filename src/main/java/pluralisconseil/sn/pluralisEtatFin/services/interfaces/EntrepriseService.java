package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseLockedException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseQuotaExceededException;

import java.util.Map;

public interface EntrepriseService extends Service<EntrepriseDto> {
    EntrepriseDto getName(String name);
    Entreprise getEntity(Long id);

    /**
     * Creates a new {@code Entreprise} scoped to the given {@code societeId}, enforcing the
     * société's current {@code Abonnement} quota and non-hybride lock (see
     * {@link EntrepriseQuotaExceededException}/{@link EntrepriseLockedException}). The existing
     * no-arg {@link #create(EntrepriseDto)} performs none of these checks and stays available
     * for internal/test callers that don't need them.
     */
    EntrepriseDto create(EntrepriseDto dto, Long societeId) throws EntrepriseQuotaExceededException, EntrepriseLockedException;

    /**
     * Same filtering as {@link #getAll(Map, Pageable)}, restricted to the given
     * {@code societeId} so a caller never sees another société's entreprises.
     */
    Page<EntrepriseDto> getAllBySociete(Long societeId, Map<String, String> searchParams, Pageable pageable);
}
