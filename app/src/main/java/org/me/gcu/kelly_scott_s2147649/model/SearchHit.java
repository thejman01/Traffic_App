package org.me.gcu.kelly_scott_s2147649.model;

/** A situation found by a search, with its distance from the search position (NaN for road searches). */
public final class SearchHit {
    public final Situation situation;
    public final double distanceKm;

    public SearchHit(Situation situation, double distanceKm) {
        this.situation = situation;
        this.distanceKm = distanceKm;
    }

    public boolean hasDistance() {
        return !Double.isNaN(distanceKm) && !Double.isInfinite(distanceKm);
    }
}
