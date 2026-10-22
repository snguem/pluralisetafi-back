package pluralisconseil.sn.pluralisEtatFin.helpers;

import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseSubstituteDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;

public interface BalanceMergeService {
    EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path) throws Exception;
    EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path, String balance_n_1_path) throws Exception;
    EtatFinancierDto configExcelFile(EtatFinancierDto dto, EntrepriseSubstituteDto entrepriseDto) throws Exception;
    void complement(boolean sage, String etatFinNpath, String etatFinN_1Path) throws Exception;
    void complement(boolean sage, String etatFinNpath, String etatFinN_1Path, String password) throws Exception;
}
