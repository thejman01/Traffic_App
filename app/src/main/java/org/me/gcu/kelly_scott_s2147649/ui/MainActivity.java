// Scott Kelly
// Student ID S2147649
// Program Bsc Computing

package org.me.gcu.kelly_scott_s2147649.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.me.gcu.kelly_scott_s2147649.viewmodel.LoadStatus;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The single Activity of the app (single Activity + Fragments + MVVM). It owns the toolbar, the status banner and
 * the navigation bar; every screen is a Fragment sharing one {@link TrafficViewModel}.
 *
 * The navigation bar is a BottomNavigationView in portrait and a NavigationRailView in landscape (two separate
 * activity_main layouts); both are NavigationBarViews so this code does not care which one it has.
 */
public class MainActivity extends AppCompatActivity {
    private static final String STATE_NAV = "nav_id";
    private static final String TAG_DETAIL = "detail";

    private TrafficViewModel viewModel;
    private NavigationBarView navBar;
    private TextView banner;
    private int currentNavId = R.id.nav_incidents;

    // update interval choices: label + seconds
    private static final String[] INTERVAL_LABELS = {"30 seconds (demo)", "5 minutes", "15 minutes", "1 hour (default)"};
    private static final long[] INTERVAL_SECONDS = {30, 5 * 60, 15 * 60, 60 * 60};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // The app is drawn edge-to-edge (enforced from Android 15), so push the toolbar's content below the status
        // bar / display cutout; its brand-coloured background still extends up behind the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });
        banner = findViewById(R.id.statusBanner);
        navBar = findViewById(R.id.navBar);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        if (savedInstanceState != null) {
            currentNavId = savedInstanceState.getInt(STATE_NAV, R.id.nav_incidents);
        }
        navBar.setSelectedItemId(currentNavId);           // visual only: the listener is attached afterwards
        navBar.setOnItemSelectedListener(item -> {
            showTopLevel(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            showTopLevel(currentNavId);                   // first start: show the incidents list
        }                                                 // otherwise the FragmentManager restores the fragments

        // Show the toolbar arrow only while a detail screen is the one on display. This is checked whenever a screen
        // becomes visible, rather than from the back stack count: with the phone's (predictive) Back button the
        // count is not yet updated when the listener runs, which left the arrow showing on the list screens.
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                if (f.getId() == R.id.fragmentContainer) setUpButton(TAG_DETAIL.equals(f.getTag()));
            }
        }, false);
        Fragment shown = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        setUpButton(shown != null && TAG_DETAIL.equals(shown.getTag()));

        viewModel.getStatus().observe(this, this::renderStatus);
        // search results depend on the data, so refresh them whenever new data arrives
        viewModel.getIncidents().observe(this, l -> viewModel.recomputeSearch());
        viewModel.getRoadworks().observe(this, l -> viewModel.recomputeSearch());
        banner.setOnClickListener(v -> viewModel.refreshNow());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NAV, currentNavId);
    }

    // ================================================================ navigation

    private void showTopLevel(int navId) {
        currentNavId = navId;
        String tag = "top_" + navId;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            Fragment current = fm.findFragmentById(R.id.fragmentContainer);
            if (current != null && tag.equals(current.getTag())) return;    // already showing it
        }
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction().replace(R.id.fragmentContainer, createTopLevel(navId), tag).commit();
    }

    private Fragment createTopLevel(int navId) {
        if (navId == R.id.nav_roadworks) return SituationListFragment.newInstance(SituationKind.ROADWORK);
        if (navId == R.id.nav_search) return new SearchFragment();
        if (navId == R.id.nav_map) return new TrafficMapFragment();
        return SituationListFragment.newInstance(SituationKind.INCIDENT);
    }

    /** Opens the detail screen for the situation currently selected in the ViewModel. */
    public void showDetail() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.fragmentContainer);
        if (current != null && TAG_DETAIL.equals(current.getTag())) return;
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, new SituationDetailFragment(), TAG_DETAIL)
                .addToBackStack(TAG_DETAIL)
                .commit();
    }

    /** Switches to the map tab showing just the given situations (null/empty list = everything). */
    public void openMapWith(List<Situation> situations, String label) {
        viewModel.showOnMap(situations, label);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (navBar.getSelectedItemId() != R.id.nav_map) {
            navBar.setSelectedItemId(R.id.nav_map);       // listener swaps in the map fragment
        } else {
            showTopLevel(R.id.nav_map);                   // already the map tab, but a detail screen may be on top
        }
    }

    private void setUpButton(boolean show) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(show);
            getSupportActionBar().setHomeButtonEnabled(show);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // ================================================================ status banner

    private void renderStatus(LoadStatus st) {
        String credit = getString(R.string.attribution);
        int colour;
        String text;
        switch (st.state) {
            case OK:
                colour = R.color.banner_ok;
                text = "Updated " + time(st.lastUpdatedMillis) + " · auto-refresh every "
                        + intervalText(viewModel.getIntervalSeconds()) + " · " + credit;
                break;
            case OFFLINE_CACHED:
                colour = R.color.banner_offline;
                text = st.message + (st.lastUpdatedMillis > 0 ? " (saved " + dateTime(st.lastUpdatedMillis) + ")" : "")
                        + " · tap to retry";
                break;
            case ERROR_NO_DATA:
                colour = R.color.banner_error;
                text = st.message + (viewModel.hasCredentials() ? "" :
                        " Add your Traffic Scotland client ID/key to local.properties.") + " · tap to retry";
                break;
            default:
                colour = R.color.banner_loading;
                text = st.message + " · " + credit;
                break;
        }
        banner.setBackgroundColor(ContextCompat.getColor(this, colour));
        banner.setText(text);
    }

    private static String time(long millis) {
        return DateTimeFormatter.ofPattern("HH:mm:ss", Locale.UK).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private static String dateTime(long millis) {
        return DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.UK).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private static String intervalText(long seconds) {
        if (seconds < 120) return seconds + " s";
        if (seconds < 7200) return (seconds / 60) + " min";
        return (seconds / 3600) + " h";
    }

    // ================================================================ menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            Toast.makeText(this, "Updating…", Toast.LENGTH_SHORT).show();
            viewModel.refreshNow();
            return true;
        } else if (id == R.id.action_interval) {
            showIntervalDialog();
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.about_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showIntervalDialog() {
        int checked = 0;
        for (int i = 0; i < INTERVAL_SECONDS.length; i++) {
            if (INTERVAL_SECONDS[i] == viewModel.getIntervalSeconds()) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_interval)
                .setSingleChoiceItems(INTERVAL_LABELS, checked, (dialog, which) -> {
                    viewModel.startAutoUpdate(INTERVAL_SECONDS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
