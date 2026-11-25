package org.me.gcu.kelly_scott_s2147649.viewmodel;

/** What the user should be told about the state of the data. */
public final class LoadStatus {
    public enum State { LOADING, OK, OFFLINE_CACHED, ERROR_NO_DATA }

    public final State state;
    public final String message;
    public final long lastUpdatedMillis;

    public LoadStatus(State state, String message, long lastUpdatedMillis) {
        this.state = state;
        this.message = message;
        this.lastUpdatedMillis = lastUpdatedMillis;
    }
}
