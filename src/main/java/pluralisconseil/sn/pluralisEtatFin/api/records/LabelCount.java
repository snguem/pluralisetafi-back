package pluralisconseil.sn.pluralisEtatFin.api.records;

/** One bar / one slice of a chart: a label (year, month, entreprise, formule...) and how many. */
public record LabelCount(String label, long count) {
}
