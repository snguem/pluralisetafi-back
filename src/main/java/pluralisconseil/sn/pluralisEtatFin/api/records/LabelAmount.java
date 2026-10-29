package pluralisconseil.sn.pluralisEtatFin.api.records;

import java.math.BigDecimal;

/** A label with an amount, for the revenue charts. */
public record LabelAmount(String label, BigDecimal montant) {
}
