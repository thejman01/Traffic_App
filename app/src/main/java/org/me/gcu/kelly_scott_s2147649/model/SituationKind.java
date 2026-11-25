package org.me.gcu.kelly_scott_s2147649.model;

/** Which of the two Traffic Scotland feeds a situation came from. */
public enum SituationKind {
    INCIDENT("Incident"),
    ROADWORK("Roadworks");

    private final String label;

    SituationKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
