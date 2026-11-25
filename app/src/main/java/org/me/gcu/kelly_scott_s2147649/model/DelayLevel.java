package org.me.gcu.kelly_scott_s2147649.model;

/** Severity of the delays caused by roadworks (DATEX II "delaysType"). Used for colour coding. */
public enum DelayLevel {
    NONE("No delay information", 0),
    DELAYS("Delays", 1),
    LONG_DELAYS("Long delays", 2),
    VERY_LONG_DELAYS("Very long delays", 3);

    private final String label;
    private final int severity;

    DelayLevel(String label, int severity) {
        this.label = label;
        this.severity = severity;
    }

    public String getLabel() {
        return label;
    }

    public int getSeverity() {
        return severity;
    }

    /** Maps the raw feed value (delays / longDelays / veryLongDelays / ...) to a level. Unknown values are tolerated. */
    public static DelayLevel fromFeed(String raw) {
        if (raw == null) return NONE;
        switch (raw.trim()) {
            case "delays":
            case "delayBetweenFiveAndTenMinutes":
            case "delayBetweenTenAndThirtyMinutes":
                return DELAYS;
            case "longDelays":
                return LONG_DELAYS;
            case "veryLongDelays":
                return VERY_LONG_DELAYS;
            default:
                return raw.toLowerCase().contains("verylong") ? VERY_LONG_DELAYS
                        : raw.toLowerCase().contains("long") ? LONG_DELAYS
                        : raw.toLowerCase().contains("delay") ? DELAYS : NONE;
        }
    }
}
