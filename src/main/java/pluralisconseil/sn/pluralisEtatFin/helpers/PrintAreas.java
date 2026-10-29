package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.util.regex.Pattern;

/** The print area of a sheet, written like Excel does ({@code A1:H38}) and stored absolute ({@code $A$1:$H$38}). */
public final class PrintAreas {

    private static final Pattern AREA = Pattern.compile("^\\$?([A-Za-z]{1,3})\\$?(\\d{1,7}):\\$?([A-Za-z]{1,3})\\$?(\\d{1,7})$");

    private PrintAreas() {
    }

    /** Null or blank means the whole sheet; anything else must be a range or an {@link IllegalArgumentException} is thrown. */
    public static String normalize(String area) {
        if (area == null || area.isBlank()) return null;
        var matcher = AREA.matcher(area.replace(" ", ""));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Zone d'impression invalide « " + area + " » : attendu par exemple A1:H38");
        }
        return "$" + matcher.group(1).toUpperCase() + "$" + matcher.group(2) + ":$" + matcher.group(3).toUpperCase() + "$" + matcher.group(4);
    }
}
