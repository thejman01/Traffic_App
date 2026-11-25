package org.me.gcu.kelly_scott_s2147649.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.kelly_scott_s2147649.data.FeedResult;
import org.me.gcu.kelly_scott_s2147649.data.TrafficRepository;
import org.me.gcu.kelly_scott_s2147649.model.SearchHit;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.me.gcu.kelly_scott_s2147649.util.SituationSearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The "ViewModel" of the MVVM architecture. It survives screen rotation, owns the background threads that
 * download/parse the feeds (replacing the starter code's runOnUiThread approach with LiveData.postValue) and
 * refreshes the data automatically at start-up and then at a regular interval.
 */
public class TrafficViewModel extends AndroidViewModel {
    private static final String TAG = "TrafficViewModel";

    public static final long DEFAULT_INTERVAL_SECONDS = 60 * 60;   // hourly
    // Default position for "near me" when the device location is unavailable: Glasgow Caledonian University
    public static final double FIXED_LAT = 55.8668;
    public static final double FIXED_LON = -4.2500;

    public enum SearchMode { ROAD, NEARBY }
    public enum Scope { ALL, INCIDENTS, ROADWORKS }

    private final TrafficRepository repository;

    // ----- threading
    private final ExecutorService ioPool = Executors.newFixedThreadPool(2);                 // one task per feed
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoUpdateTask;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private long intervalSeconds = DEFAULT_INTERVAL_SECONDS;

    // ----- data
    private final MutableLiveData<List<Situation>> incidents = new MutableLiveData<>(Collections.<Situation>emptyList());
    private final MutableLiveData<List<Situation>> roadworks = new MutableLiveData<>(Collections.<Situation>emptyList());
    private final MutableLiveData<LoadStatus> status =
            new MutableLiveData<>(new LoadStatus(LoadStatus.State.LOADING, "Loading…", 0));

    // ----- selection / map
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<List<Situation>> mapSelection = new MutableLiveData<>();   // null = show all
    private final MutableLiveData<String> mapSelectionLabel = new MutableLiveData<>("");

    // ----- search state (kept here so it survives rotation)
    private final MutableLiveData<List<SearchHit>> searchResults = new MutableLiveData<>(Collections.<SearchHit>emptyList());
    private SearchMode searchMode = SearchMode.ROAD;
    private Scope scope = Scope.ALL;
    private String roadQuery = "";
    private int radiusKm = 10;
    private double userLat = FIXED_LAT;
    private double userLon = FIXED_LON;
    private boolean usingDeviceLocation = false;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        startAutoUpdate(DEFAULT_INTERVAL_SECONDS);   // first run happens immediately = update at start-up
    }

    // ================================================================ public LiveData

    public LiveData<List<Situation>> getIncidents() { return incidents; }
    public LiveData<List<Situation>> getRoadworks() { return roadworks; }
    public LiveData<LoadStatus> getStatus() { return status; }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<List<Situation>> getMapSelection() { return mapSelection; }
    public LiveData<String> getMapSelectionLabel() { return mapSelectionLabel; }
    public LiveData<List<SearchHit>> getSearchResults() { return searchResults; }

    public boolean hasCredentials() { return repository.hasCredentials(); }

    // ================================================================ refresh + auto update

    /** (Re)starts the periodic refresh. The first refresh runs immediately. */
    public synchronized void startAutoUpdate(long seconds) {
        intervalSeconds = seconds;
        if (autoUpdateTask != null) autoUpdateTask.cancel(false);
        autoUpdateTask = scheduler.scheduleWithFixedDelay(this::refreshBlocking, 0, seconds, TimeUnit.SECONDS);
    }

    public long getIntervalSeconds() { return intervalSeconds; }

    /** Manual refresh (pull / menu). Runs on the scheduler thread, never on the UI thread. */
    public void refreshNow() {
        scheduler.execute(this::refreshBlocking);
    }

    /** Runs on a background thread: downloads both feeds in parallel, then publishes the results to LiveData. */
    private void refreshBlocking() {
        if (!refreshing.compareAndSet(false, true)) return;   // a refresh is already running
        try {
            LoadStatus previous = status.getValue();
            status.postValue(new LoadStatus(LoadStatus.State.LOADING, "Updating…",
                    previous == null ? 0 : previous.lastUpdatedMillis));

            Future<FeedResult> incidentsTask = ioPool.submit(() -> repository.loadFeed(SituationKind.INCIDENT));
            Future<FeedResult> roadworksTask = ioPool.submit(() -> repository.loadFeed(SituationKind.ROADWORK));
            publish(incidentsTask.get(), roadworksTask.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, "Refresh failed", e);
            status.postValue(new LoadStatus(LoadStatus.State.ERROR_NO_DATA, "Update failed: " + e.getMessage(), 0));
        } finally {
            refreshing.set(false);
        }
    }

    private void publish(FeedResult inc, FeedResult rw) {
        // keep what we already have if a feed produced nothing at all
        if (inc.hasData()) incidents.postValue(inc.situations);
        if (rw.hasData()) roadworks.postValue(rw.situations);

        boolean anyData = inc.hasData() || rw.hasData();
        long newest = Math.max(inc.dataTimeMillis, rw.dataTimeMillis);
        LoadStatus next;
        if (!anyData) {
            String why = inc.error != null ? inc.error : rw.error;
            boolean offline = TrafficRepository.NO_CONNECTION.equals(why);
            next = new LoadStatus(LoadStatus.State.ERROR_NO_DATA,
                    "No data available (" + why + ")." + (offline ? " Check your connection." : ""), 0);
        } else if (inc.fromCache || rw.fromCache || inc.error != null || rw.error != null) {
            String why = inc.error != null ? inc.error : rw.error;
            boolean offline = TrafficRepository.NO_CONNECTION.equals(why);
            next = new LoadStatus(LoadStatus.State.OFFLINE_CACHED,
                    (offline ? "Offline (" : "Could not update (") + why + ") – showing saved data", Math.min(
                    inc.dataTimeMillis == 0 ? Long.MAX_VALUE : inc.dataTimeMillis,
                    rw.dataTimeMillis == 0 ? Long.MAX_VALUE : rw.dataTimeMillis));
        } else {
            next = new LoadStatus(LoadStatus.State.OK, "Up to date", newest);
        }
        status.postValue(next);   // (MainActivity re-runs the search when the data lists change)
    }

    // ================================================================ selection

    public void select(Situation s) { selected.setValue(s); }

    /** Map shows just these situations (e.g. search results or the one being viewed). */
    public void showOnMap(List<Situation> situations, String label) {
        mapSelection.setValue(new ArrayList<>(situations));
        mapSelectionLabel.setValue(label);
    }

    /** Map shows everything again. */
    public void showAllOnMap() {
        mapSelection.setValue(null);
        mapSelectionLabel.setValue("");
    }

    // ================================================================ search

    public SearchMode getSearchMode() { return searchMode; }
    public Scope getScope() { return scope; }
    public String getRoadQuery() { return roadQuery; }
    public int getRadiusKm() { return radiusKm; }
    public double getUserLat() { return userLat; }
    public double getUserLon() { return userLon; }
    public boolean isUsingDeviceLocation() { return usingDeviceLocation; }

    public void setSearchMode(SearchMode mode) { searchMode = mode; recomputeSearch(); }
    public void setScope(Scope s) { scope = s; recomputeSearch(); }
    public void setRoadQuery(String q) { roadQuery = q == null ? "" : q; recomputeSearch(); }
    public void setRadiusKm(int km) { radiusKm = km; recomputeSearch(); }

    public void setLocation(double lat, double lon, boolean fromDevice) {
        userLat = lat;
        userLon = lon;
        usingDeviceLocation = fromDevice;
        recomputeSearch();
    }

    public void useFixedLocation() { setLocation(FIXED_LAT, FIXED_LON, false); }

    /** Must be called on the main thread. */
    public void recomputeSearch() {
        List<Situation> pool = new ArrayList<>();
        if (scope != Scope.ROADWORKS) pool.addAll(nonNull(incidents.getValue()));
        if (scope != Scope.INCIDENTS) pool.addAll(nonNull(roadworks.getValue()));

        List<SearchHit> hits = searchMode == SearchMode.ROAD
                ? SituationSearch.byRoad(pool, roadQuery)
                : SituationSearch.nearby(pool, userLat, userLon, radiusKm);
        searchResults.setValue(hits);
    }

    private static List<Situation> nonNull(List<Situation> l) {
        return l == null ? Collections.<Situation>emptyList() : l;
    }

    // ================================================================ lifecycle

    @Override
    protected void onCleared() {
        scheduler.shutdownNow();
        ioPool.shutdownNow();
        super.onCleared();
    }
}
