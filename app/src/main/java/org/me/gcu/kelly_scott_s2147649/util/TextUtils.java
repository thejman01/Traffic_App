package org.me.gcu.kelly_scott_s2147649.util;

import java.util.Locale;

/** Text helpers for turning feed values into something readable. */
public final class TextUtils {
    private TextUtils() {}

    /** "roadOrCarriagewayOrLaneManagement" / "roadClosed" -> "Road closed". */
    public static String humanise(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        String spaced = camelCase.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").toLowerCase(Locale.ROOT).trim();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
