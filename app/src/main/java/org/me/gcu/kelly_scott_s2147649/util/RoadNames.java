package org.me.gcu.kelly_scott_s2147649.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracting and matching Scottish road names such as M8, A74(M) or B957. */
public final class RoadNames {
    // M8, A90, B957, A74(M), A74 (M)
    private static final Pattern ROAD = Pattern.compile("\\b([ABM]\\d{1,4}(?:\\s?\\(M\\))?)(?![\\w])");

    private RoadNames() {}

    /** Display form: upper case, no inner spaces ("a74 (m)" -> "A74(M)"). */
    public static String canonical(String road) {
        return road == null ? "" : road.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** Key form used for comparisons: letters and digits only ("A74(M)" -> "A74M"). */
    public static String key(String road) {
        return road == null ? "" : road.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    /** Finds road names inside free text such as "The A78 northbound at Barrassie ...". */
    public static Set<String> extractFromText(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null) return found;
        Matcher m = ROAD.matcher(text);
        while (m.find()) found.add(canonical(m.group(1)));
        return found;
    }

    /**
     * Does a road (e.g. "A74(M)") match what the user typed (e.g. "a74" or "A74M")?
     * "A7" must NOT match "A74(M)" and "M8" must NOT match "M80", but "A74" matches "A74(M)".
     */
    public static boolean matches(String road, String query) {
        String r = key(road);
        String q = key(query);
        if (q.isEmpty()) return false;
        if (r.equals(q)) return true;
        if (!r.startsWith(q)) return false;
        boolean queryEndsWithDigit = Character.isDigit(q.charAt(q.length() - 1));
        return !(queryEndsWithDigit && Character.isDigit(r.charAt(q.length())));
    }
}
