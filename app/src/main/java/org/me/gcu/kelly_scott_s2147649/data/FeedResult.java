package org.me.gcu.kelly_scott_s2147649.data;

import org.me.gcu.kelly_scott_s2147649.model.Situation;

import java.util.Collections;
import java.util.List;

/** Outcome of loading one feed: either fresh data, cached data (when offline), or an error with no data. */
public final class FeedResult {
    public final List<Situation> situations;
    public final long dataTimeMillis;     // when the data was downloaded
    public final boolean fromCache;
    public final String error;            // null when the feed could be loaded (from network or cache)

    private FeedResult(List<Situation> situations, long dataTimeMillis, boolean fromCache, String error) {
        this.situations = situations;
        this.dataTimeMillis = dataTimeMillis;
        this.fromCache = fromCache;
        this.error = error;
    }

    public static FeedResult fresh(List<Situation> s, long time) {
        return new FeedResult(s, time, false, null);
    }

    public static FeedResult cached(List<Situation> s, long time, String networkProblem) {
        return new FeedResult(s, time, true, networkProblem);
    }

    public static FeedResult failed(String error) {
        return new FeedResult(Collections.<Situation>emptyList(), 0L, false, error);
    }

    public boolean hasData() {
        return !situations.isEmpty() || dataTimeMillis > 0;
    }
}
