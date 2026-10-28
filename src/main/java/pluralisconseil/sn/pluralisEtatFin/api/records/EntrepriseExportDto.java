package pluralisconseil.sn.pluralisEtatFin.api.records;

import pluralisconseil.sn.pluralisEtatFin.api.models.ActionaireEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.BanqueEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;

import java.util.List;

/**
 * Full data export of an {@code Entreprise}: the entreprise itself plus every one of its child
 * collections (dirigeants, banques, actionnaires, activites, gerants), produced by
 * {@code EntrepriseService#export(Long)} for the {@code GET /entreprises/{id}/export} endpoint.
 */
public record EntrepriseExportDto(
        EntrepriseDto entreprise,
        List<DirigeantEntrepriseDto> dirigeants,
        List<BanqueEntrepriseDto> banques,
        List<ActionaireEntrepriseDto> actionaires,
        List<ActiviteEntrepriseDto> activites,
        List<GerantEntrepriseDto> gerants
) {
}
