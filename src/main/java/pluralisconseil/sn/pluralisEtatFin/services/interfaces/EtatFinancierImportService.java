package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;

/**
 * Orchestre l'import des fichiers de balance (et, le cas echeant, de l'etat financier N-1)
 * dans l'etat financier identifie par son id : sauvegarde des fichiers en temporaire, fusion
 * dans le fichier excel du modele, configuration des valeurs, nettoyage des feuilles et
 * recalcul des formules, puis mise a jour de l'enregistrement.
 * <p>
 * En cas d'echec (fichier manquant ou erreur de fusion/configuration), l'implementation
 * supprime l'enregistrement {@code EtatFinancier} correspondant avant de relancer l'exception.
 */
public interface EtatFinancierImportService {
    EtatFinancierDto importBalanceN(Long etatFinancierId, MultipartFile balanceN) throws Exception;

    EtatFinancierDto importBalances(Long etatFinancierId, MultipartFile balanceN, MultipartFile balanceN1) throws Exception;

    EtatFinancierDto importBalancesWithEtatFinancierN1(Long etatFinancierId, MultipartFile balanceN, MultipartFile balanceN1, MultipartFile etatFinancierN1) throws Exception;
}
