package pluralisconseil.sn.pluralisEtatFin.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: no Spring context, no database. */
class PdfPageRulesTest {

    @Test
    void legacyRules_keepTheHardCodedBehaviourOfTheRealModelSheets() {
        // values the hard-coded chain gave for the sheets of a real model, before it became configurable
        assertThat(LegacyPdfPageRules.forSheet("BILAN PAYSAGE")).isEqualTo(new PdfPageSettings("$A$1:$L$36", true));
        assertThat(LegacyPdfPageRules.forSheet("COMPTE DE RESULTAT")).isEqualTo(new PdfPageSettings("$A$1:$G$48", false));
        assertThat(LegacyPdfPageRules.forSheet("FLUX DE TRESORERIE")).isEqualTo(new PdfPageSettings("$A$1:$F$36", false));
        assertThat(LegacyPdfPageRules.forSheet("Fiche de renseignement R1")).isEqualTo(new PdfPageSettings("$A$1:$AN$84", false));
        assertThat(LegacyPdfPageRules.forSheet("Note 1")).isEqualTo(new PdfPageSettings("$A$1:$F$43", false));
        assertThat(LegacyPdfPageRules.forSheet("Note 3B")).isEqualTo(new PdfPageSettings("$A$1:$I$20", true));
        assertThat(LegacyPdfPageRules.forSheet("Note 4")).isEqualTo(new PdfPageSettings("$A$1:$G$31", true));
        assertThat(LegacyPdfPageRules.forSheet("Note 27B")).isEqualTo(new PdfPageSettings("$A$1:$P$30", true));
    }

    @Test
    void legacyRules_ignoreTheCaseOfTheSheetName() {
        assertThat(LegacyPdfPageRules.forSheet("bilan paysage")).isEqualTo(LegacyPdfPageRules.forSheet("BILAN PAYSAGE"));
    }

    @Test
    void legacyRules_printTheWholeSheetWhenNoRuleApplies() {
        assertThat(LegacyPdfPageRules.forSheet("Page de garde")).isEqualTo(new PdfPageSettings(null, false));
        assertThat(LegacyPdfPageRules.forSheet(null)).isEqualTo(new PdfPageSettings(null, false));
    }

    @Test
    void printAreas_normalizeToAbsoluteReferences() {
        assertThat(PrintAreas.normalize("A1:H38")).isEqualTo("$A$1:$H$38");
        assertThat(PrintAreas.normalize(" a1 : ah84 ")).isEqualTo("$A$1:$AH$84");
        assertThat(PrintAreas.normalize("$A$1:$H$38")).isEqualTo("$A$1:$H$38");
    }

    @Test
    void printAreas_blankMeansTheWholeSheet() {
        assertThat(PrintAreas.normalize(null)).isNull();
        assertThat(PrintAreas.normalize("   ")).isNull();
    }

    @Test
    void printAreas_rejectAnythingThatIsNotARange() {
        assertThatThrownBy(() -> PrintAreas.normalize("A1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PrintAreas.normalize("tout")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PrintAreas.normalize("A1:H")).isInstanceOf(IllegalArgumentException.class);
    }
}
