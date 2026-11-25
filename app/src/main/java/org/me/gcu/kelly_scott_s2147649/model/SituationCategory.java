package org.me.gcu.kelly_scott_s2147649.model;

/** The kinds of situation we show an icon for (7 different icons). */
public enum SituationCategory {
    ROAD_CLOSED("Road closed"),
    LANE_RESTRICTION("Lane restriction"),
    QUEUING_TRAFFIC("Queuing traffic"),
    SLOW_TRAFFIC("Slow / heavy traffic"),
    ROADWORKS("Roadworks"),
    ROADWORKS_CLOSURE("Roadworks with closure"),
    OTHER("Other incident");

    private final String label;

    SituationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
