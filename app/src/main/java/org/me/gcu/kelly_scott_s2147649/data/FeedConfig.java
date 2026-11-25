package org.me.gcu.kelly_scott_s2147649.data;

/** Locations of the two Traffic Scotland DATEX II feeds used by the app. */
public final class FeedConfig {
    private FeedConfig() {}

    public static final String UNPLANNED_EVENTS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    public static final String CURRENT_ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    public static final int CONNECT_TIMEOUT_MS = 15_000;
    public static final int READ_TIMEOUT_MS = 45_000;
}
