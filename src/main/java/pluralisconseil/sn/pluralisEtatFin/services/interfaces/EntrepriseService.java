package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.EntrepriseExportDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseLockedException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseQuotaExceededException;

import java.util.Map;

public interface EntrepriseService extends Service<EntrepriseDto> {
    EntrepriseDto getName(String name);
    Entreprise getEntity(Long id);

    /** Name lookup restricted to one société, so names are unique per société and never leak across them. */
    EntrepriseDto getName(String name, Long societeId);

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

    /**
     * Full data export of the {@code Entreprise}: itself plus every child collection (dirigeants,
     * banques, actionnaires, activites, gerants).
     */
    EntrepriseExportDto export(Long entrepriseId);

    /**
     * Clears every child collection of the {@code Entreprise} (dirigeants, banques, actionnaires,
     * activites, gerants) and resets its {@code substitute} to keep only {@code nameEntreprise}
     * and {@code code_importeur}, nulling every other field.
     */
    EntrepriseDto wipeExceptIdentity(Long entrepriseId);
}
