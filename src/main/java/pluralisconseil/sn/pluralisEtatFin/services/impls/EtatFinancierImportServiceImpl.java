package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseSubstituteDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.helpers.BalanceMergeService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelCleanupService;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelFormulaCalculator;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierImportService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EtatFinancierService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import java.util.Arrays;
import java.util.List;

/**
 * Orchestrates the balance-import workflow currently duplicated inline across
 * {@code EtatFinancierRestController#setBalances}, {@code #setBalancesWithEtatFinN1},
 * {@code #setBalanceN} and the private helper {@code #getObjectResponse}. This class replicates
 * that logic exactly (including its current quirks), and additionally owns the
 * delete-the-record-on-failure behavior that those methods currently perform in their
 * {@code catch} blocks: on any failure here, the {@code EtatFinancier} record is deleted before
 * the triggering exception is rethrown, leaving it to the caller (the controller) to translate
 * the exception into an HTTP {@code Response}.
 * <p>
 * One failure mode is deliberately excluded from that delete: in the controller's current
 * {@code setBalances}/{@code setBalancesWithEtatFinN1}, the "missing temp balance file" branch
 * does a plain {@code return Response.exception()...} <em>inside</em> the {@code try} block, so
 * it never reaches the method's {@code catch} and the record is NOT deleted on that specific
 * path. {@link BalanceMergeUnavailableException} is used internally to signal exactly that
 * branch so it can be rethrown without deleting, preserving that exact (if perhaps accidental)
 * behavior.
 */
@Service
@RequiredArgsConstructor
public class EtatFinancierImportServiceImpl implements EtatFinancierImportService {
    private final EtatFinancierService etatFinancierService;
    private final ModelExcelService modelExcelService;
    private final HelperService helperService;
    private final BalanceMergeService balanceMergeService;
    private final ExcelCleanupService excelCleanupService;
    private final ExcelFormulaCalculator excelFormulaCalculator;

    /**
     * Internal signal for the "missing temp balance file" branch of
     * {@link #importBalances} / {@link #importBalancesWithEtatFinancierN1}, which the
     * controller currently handles with a direct {@code return} (bypassing its {@code catch}
     * blocks, hence no {@code service.delete(id)}) rather than a thrown exception caught
     * further up.
     */
    private static final class BalanceMergeUnavailableException extends RuntimeException {
        BalanceMergeUnavailableException(String message) {
            super(message);
        }
    }

    @Override
    public EtatFinancierDto importBalanceN(Long etatFinancierId, MultipartFile balanceN) throws Exception {
        try {
            var dto = etatFinancierService.get(etatFinancierId);
            var model = modelExcelService.get(dto.getModelExcel_id());
            EntrepriseSubstituteDto entreprise = dto.getEntreprise();

            String balanceNTemp = helperService.saveTempFile(balanceN);

            EtatFinancierDto fused = balanceMergeService.fuseBalanceToExcel(dto, model.getExcelPath(), balanceNTemp);

            var finalEtatFin = balanceMergeService.configExcelFile(fused, entreprise);
            if (finalEtatFin != null) {
                var updated = etatFinancierService.update(finalEtatFin);
                helperService.removesTemp(List.of(balanceNTemp));
                return updated;
            } else {
                throw new Exception("Une erreur s'est produite lors de la configuration des fichiers");
            }
        } catch (Exception ex) {
            etatFinancierService.delete(etatFinancierId);
            throw ex;
        }
    }

    @Override
    public EtatFinancierDto importBalances(Long etatFinancierId, MultipartFile balanceN, MultipartFile balanceN1) throws Exception {
        try {
            var dto = etatFinancierService.get(etatFinancierId);
            var model = modelExcelService.get(dto.getModelExcel_id());
            EntrepriseSubstituteDto entreprise = dto.getEntreprise();

            String balanceNTemp = helperService.saveTempFile(balanceN);
            String balanceN1Temp = helperService.saveTempFile(balanceN1);

            EtatFinancierDto fused;
            if (balanceN1Temp != null && balanceNTemp != null) {
                fused = balanceMergeService.fuseBalanceToExcel(dto, model.getExcelPath(), balanceNTemp, balanceN1Temp);
            } else {
                helperService.removesTemp(Arrays.asList(balanceNTemp, balanceN1Temp));
                throw new BalanceMergeUnavailableException("Impossible de fusionner les balances");
            }

            return finalizeFusedImport(entreprise, balanceNTemp, balanceN1Temp, fused);
        } catch (BalanceMergeUnavailableException ex) {
            // Matches the controller's current `return Response.exception()...` on this branch:
            // no service.delete(etatFinancierId) call here.
            throw ex;
        } catch (Exception ex) {
            etatFinancierService.delete(etatFinancierId);
            throw ex;
        }
    }

    @Override
    public EtatFinancierDto importBalancesWithEtatFinancierN1(Long etatFinancierId, MultipartFile balanceN, MultipartFile balanceN1, MultipartFile etatFinancierN1) throws Exception {
        try {
            var dto = etatFinancierService.get(etatFinancierId);
            var model = modelExcelService.get(dto.getModelExcel_id());
            EntrepriseSubstituteDto entreprise = dto.getEntreprise();

            String balanceNTemp = helperService.saveTempFile(balanceN);
            String balanceN1Temp = helperService.saveTempFile(balanceN1);
            String etatFinN1Temp = helperService.saveTempFile(etatFinancierN1);

            EtatFinancierDto fused;
            if (balanceN1Temp != null && balanceNTemp != null) {
                fused = balanceMergeService.fuseBalanceToExcel(dto, model.getExcelPath(), balanceNTemp, balanceN1Temp);
            } else {
                helperService.removesTemp(Arrays.asList(balanceNTemp, balanceN1Temp));
                throw new BalanceMergeUnavailableException("Impossible de fusionner les balances");
            }

            // Exact replica of the controller's current behavior: complement() is invoked with
            // the ORIGINAL dto's excel path (fetched before the fuse above), not the freshly
            // fused one, and there is no null-guard on etatFinN1Temp before calling isEmpty().
            if (!etatFinN1Temp.isEmpty())
                balanceMergeService.complement(false, dto.getExcelPath(), etatFinN1Temp);

            return finalizeFusedImport(entreprise, balanceNTemp, balanceN1Temp, fused);
        } catch (BalanceMergeUnavailableException ex) {
            // Matches the controller's current `return Response.exception()...` on this branch:
            // no service.delete(etatFinancierId) call here.
            throw ex;
        } catch (Exception ex) {
            etatFinancierService.delete(etatFinancierId);
            throw ex;
        }
    }

    /**
     * Replicates the controller's private {@code getObjectResponse} helper: configures the
     * excel file, cleans up its sheets, forces formula recalculation, persists the update and
     * removes the temp balance files.
     */
    private EtatFinancierDto finalizeFusedImport(EntrepriseSubstituteDto entreprise, String balanceNTemp, String balanceN1Temp, EtatFinancierDto fused) throws Exception {
        var finalEtatFin = balanceMergeService.configExcelFile(fused, entreprise);
        if (finalEtatFin != null) {
            excelCleanupService.nettoyerEtRecupererExcel(finalEtatFin.getExcelPath());
            excelFormulaCalculator.calculateFormule(finalEtatFin.getExcelPath());
            var updated = etatFinancierService.update(finalEtatFin);
            helperService.removesTemp(List.of(balanceNTemp, balanceN1Temp));
            return updated;
        } else {
            throw new Exception("Une erreur s'est produitehgd lors de la configuration des fichiers");
        }
    }
}
