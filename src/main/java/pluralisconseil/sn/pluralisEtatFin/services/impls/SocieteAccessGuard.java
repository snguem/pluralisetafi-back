package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserSocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.UnauthorizeException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

/**
 * Single place deciding whether the caller's société owns an {@code Entreprise}; every
 * controller handling data hanging off an entreprise goes through it.
 */
@Component
@RequiredArgsConstructor
public class SocieteAccessGuard {

    private static final String ADMIN_COMPTE = "ADMIN_COMPTE";
    private static final String COLLABORATEUR = "COLLABORATEUR";

    /** Everything that changes data: ADMIN_COMPTE and COLLABORATEUR; INVITE is read-only. */
    public static void requireWrite(UserSocieteDto caller) {
        if (!ADMIN_COMPTE.equals(caller.getRole()) && !COLLABORATEUR.equals(caller.getRole())) {
            throw new UnauthorizeException("Votre rôle est en lecture seule");
        }
    }

    /** Société-level decisions (creating or deleting an entreprise, ...): ADMIN_COMPTE only. */
    public static void requireAdminCompte(UserSocieteDto caller) {
        if (!ADMIN_COMPTE.equals(caller.getRole())) {
            throw new UnauthorizeException("Action réservée à l'administrateur de la société");
        }
    }


    private final UserSocieteService userSocieteService;
    private final EntrepriseRepository entrepriseRepository;

    public UserSocieteDto caller(Authentication authentication) {
        try {
            return userSocieteService.resolveCurrent(authentication);
        } catch (SocieteMembershipNotFoundException ex) {
            throw new UnauthorizeException(ex.getMessage());
        }
    }

    /** @throws UnauthorizeException if the entreprise is unknown or belongs to another société */
    public void assertEntrepriseInSociete(UserSocieteDto caller, Long entrepriseId) {
        if (entrepriseId == null) {
            throw new UnauthorizeException("Accès refusé à cette entreprise");
        }
        var entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise == null || entreprise.getSociete() == null
                || !entreprise.getSociete().getId().equals(caller.getSocieteId())) {
            throw new UnauthorizeException("Accès refusé à cette entreprise");
        }
    }
}
