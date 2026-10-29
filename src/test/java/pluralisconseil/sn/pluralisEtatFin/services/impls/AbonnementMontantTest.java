package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** The amount of an abonnement: (price per month + the hybride option when it is extra) x months. */
class AbonnementMontantTest {

    private static AbonnementPlan plan(boolean hybrideInclus) {
        return AbonnementPlan.builder().prixParMois(new BigDecimal("25000"))
                .hybrideSupplementPrix(new BigDecimal("15000")).hybrideInclus(hybrideInclus).build();
    }

    @Test
    void withoutTheOption() {
        assertThat(AbonnementServiceImpl.montant(plan(false), false, 3)).isEqualByComparingTo("75000");
    }

    @Test
    void theOptionIsAddedEachMonthWhenItIsExtra() {
        assertThat(AbonnementServiceImpl.montant(plan(false), true, 3)).isEqualByComparingTo("120000");
    }

    @Test
    void anIncludedOptionCostsNothingMore() {
        assertThat(AbonnementServiceImpl.montant(plan(true), true, 3)).isEqualByComparingTo("75000");
    }
}
