package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.MeSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.PlatformStatsDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.PublicPlanDto;
import pluralisconseil.sn.pluralisEtatFin.api.records.SocieteStatsDto;

import java.util.List;

/** Read-only figures for the frontend: public landing page, société shell and dashboards. */
public interface DashboardService {

    /** The active abonnement formulas, cheapest first. Public. */
    List<PublicPlanDto> publicPlans();

    /** Société, role and abonnement of the signed-in user. */
    MeSocieteDto meSociete(UserSocieteDto caller);

    /** Dashboard figures of the caller's société. */
    SocieteStatsDto societeStats(UserSocieteDto caller);

    /** Dashboard figures of the whole platform. */
    PlatformStatsDto platformStats();
}
