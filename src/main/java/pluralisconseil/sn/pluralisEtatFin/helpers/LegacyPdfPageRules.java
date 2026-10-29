package pluralisconseil.sn.pluralisEtatFin.helpers;

/**
 * The print rules that were hard-coded for every Excel model: from the name of a sheet, the area
 * to print and the orientation. They are kept as the fallback for a sheet that a model does not
 * configure itself (see {@code ModelPdfPage}), so the PDF of an existing model does not change.
 * Order matters: the first rule whose text the sheet name contains wins.
 */
public final class LegacyPdfPageRules {

    private LegacyPdfPageRules() {
    }

    /** The settings of a sheet; {@code printArea} is null when no rule applies (the whole sheet is printed). */
    public static PdfPageSettings forSheet(String sheetNameAnyCase) {
        String sheetName = sheetNameAnyCase == null ? "" : sheetNameAnyCase.toLowerCase();
        String area = null;
        boolean landscape = false;

        if (sheetName.contains("36"))
            area = "$A$1:$H$38";
        else if (sheetName.contains("35"))
            area = "$A$1:$D$25";
        else if (sheetName.contains("34"))
            area = "$A$1:$D$62";
        else if (sheetName.contains("31"))
            area = "$A$1:$F$29";
        else if (sheetName.contains("32"))
            area = "$A$1:$N$25";
        else if (sheetName.contains("30"))
            area = "$A$1:$D$29";
        else if (sheetName.contains("29"))
            area = "$A$1:$D$31";
        else if (sheetName.contains("28"))
            area = "$A$1:$I$27";
        else if (sheetName.contains("27b")){
            area = "$A$1:$P$30";
            landscape = true;
        }
        else if (sheetName.contains("27a"))
            area = "$A$1:$D$15";
        else if (sheetName.contains("26"))
            area = "$A$1:$D$17";
        else if (sheetName.contains("25"))
            area = "$A$1:$D$14";
        else if (sheetName.contains("24"))
            area = "$A$1:$D$23";
        else if (sheetName.contains("23"))
            area = "$A$1:$D$13";
        else if (sheetName.contains("22"))
            area = "$A$1:$D$32";
        else if (sheetName.contains("21"))
            area = "$A$1:$D$36";
        else if (sheetName.contains("20"))
            area = "$A$1:$D$19";
        else if (sheetName.contains("19")){
            area = "$A$1:$H$28";
            landscape = false;
        }
        else if (sheetName.contains("18"))
            area = "$A$1:$H$23";
        else if (sheetName.contains("17"))
            area = "$A$1:$G$18";
        else if (sheetName.contains("16c"))
            area = "$A$1:$D$15";
        else if (sheetName.contains("16b bis"))
            area = "$A$1:$E$22";
        else if (sheetName.contains("16b"))
            area = "$A$1:$F$40";
        else if (sheetName.contains("16a"))
            area = "$A$1:$H$38";
        else if (sheetName.contains("15b"))
            area = "$A$1:$G$14";
        else if (sheetName.contains("15a"))
            area = "$A$1:$H$26";
        else if (sheetName.contains("14"))
            area = "$A$1:$G$23";
        else if (sheetName.contains("13"))
            area = "$A$1:$G$19";
        else if (sheetName.contains("12"))
            area = "$A$1:$G$32";
        else if (sheetName.contains("11"))
            area = "$A$1:$D$23";
        else if (sheetName.contains("10"))
            area = "$A$1:$D$16";
        else if (sheetName.contains("9"))
            area = "$A$1:$D$17";
        else if (sheetName.contains("8"))
            area = "$A$1:$G$21";
        else if (sheetName.contains("7"))
            area = "$A$1:$G$23";
        else if (sheetName.contains("6"))
            area = "$A$1:$D$19";
        else if (sheetName.contains("5"))
            area = "$A$1:$G$28";
        else if (sheetName.contains("4") && !sheetName.contains("r4")){
            area = "$A$1:$G$31";
            landscape = true;
        }
        else if (sheetName.contains("3d"))
            area = "$A$1:$F$25";
        else if (sheetName.contains("3c")){
            area = "$A$1:$E$24";
            landscape = false;
        }
        else if (sheetName.contains("3b")){
            area = "$A$1:$I$20";
            landscape = true;
        }
        else if (sheetName.contains("3a")){
            area = "$A$1:$H$27";
            landscape = false;
        }
        else if (sheetName.contains("r4")){
            area = "$A$1:$F$54";
            landscape = false;
        }
        else if (sheetName.contains("r2"))
            area = "$A$1:$S$60";
        else if (sheetName.contains("r3"))
            area = "$A$1:$F$50";
        else if (sheetName.contains("r1"))
            area = "$A$1:$AN$84";
        else if (sheetName.contains("2"))
            area = "$A$1:$D$13";
        else if (sheetName.contains("1"))
            area = "$A$1:$F$43";
        else if (sheetName.contains("flux"))
            area = "$A$1:$F$36";
        else if (sheetName.contains("compte"))
            area = "$A$1:$G$48";
        else if (sheetName.contains("bilan")){
            area = "$A$1:$L$36";
            landscape = true;
        }

        return new PdfPageSettings(area, landscape);
    }
}
