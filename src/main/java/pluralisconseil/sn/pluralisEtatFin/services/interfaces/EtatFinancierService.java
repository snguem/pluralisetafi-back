package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;

import java.util.Map;

public interface EtatFinancierService extends Service<EtatFinancierDto> {
    Page<EtatFinancierDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);

    /**
     * Same filtering as {@link #getAll(Map, Pageable)}, restricted to the real
     * {@code entrepriseRef.societe.id} relation (not the embedded, possibly-stale
     * {@code entreprise} snapshot) so a caller never sees another société's états financiers.
     */
    Page<EtatFinancierDto> getAllBySociete(Long societeId, Map<String, String> searchParams, Pageable pageable);

    /**
     * Deletes every {@code EtatFinancier} whose {@code entrepriseRef} belongs to the given
     * société, and no others.
     */
    void deleteAllBySociete(Long societeId);

    /**
     * Returns the raw {@code EtatFinancier} entity (not the DTO), so callers can navigate
     * {@code entrepriseRef.societe.id} for ownership checks (matching
     * {@code EntrepriseService#getEntity}'s existing shape).
     */
    EtatFinancier getEntity(Long id);
}
