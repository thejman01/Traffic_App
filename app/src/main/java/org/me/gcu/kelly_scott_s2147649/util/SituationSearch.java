package org.me.gcu.kelly_scott_s2147649.util;

import org.me.gcu.kelly_scott_s2147649.model.SearchHit;
import org.me.gcu.kelly_scott_s2147649.model.Situation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure-Java search functions (road name and distance), unit tested on the JVM. */
public final class SituationSearch {
    private SituationSearch() {}

    /**
     * Situations on a road. "A74", "a74m" and "A74 (M)" all find A74(M). Queries without digits
     * (e.g. "Brechin") are treated as a place name and searched for in the description text.
     */
    public static List<SearchHit> byRoad(List<Situation> all, String query) {
        List<SearchHit> hits = new ArrayList<>();
        if (all == null || query == null) return hits;
        String q = query.trim();
        if (q.isEmpty()) return hits;
        boolean roadLike = q.matches(".*\\d.*");
        String lower = q.toLowerCase(Locale.ROOT);
        for (Situation s : all) {
            boolean match = false;
            for (String road : s.getRoadNames()) {
                if (RoadNames.matches(road, q)) {
                    match = true;
                    break;
                }
            }
            if (!match && !roadLike && lower.length() >= 3) {
                match = s.getSearchableText().contains(lower);
            }
            if (match) hits.add(new SearchHit(s, Double.NaN));
        }
        return hits;
    }

    /** Situations within radiusKm of a position, nearest first. Situations without coordinates are skipped. */
    public static List<SearchHit> nearby(List<Situation> all, double lat, double lon, double radiusKm) {
        List<SearchHit> hits = new ArrayList<>();
        if (all == null) return hits;
        for (Situation s : all) {
            double d = s.distanceKmFrom(lat, lon);
            if (d <= radiusKm) hits.add(new SearchHit(s, d));
        }
        Collections.sort(hits, (a, b) -> Double.compare(a.distanceKm, b.distanceKm));
        return hits;
    }
}
