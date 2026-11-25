package org.me.gcu.kelly_scott_s2147649.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.SearchHit;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel.Scope;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel.SearchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search by road name and search by distance from the user's (coarse) location or a fixed position. */
public class SearchFragment extends Fragment {

    private TrafficViewModel vm;
    private SituationAdapter adapter;
    private boolean binding;           // true while we set widget values from the ViewModel (ignore their listeners)
    private String lastSearchKey;      // the search the list is currently showing (to know when to scroll to the top)

    private MaterialButtonToggleGroup modeToggle;
    private ChipGroup scopeGroup;
    private View roadPanel, nearbyPanel;
    private TextInputEditText roadInput;
    private TextView radiusLabel, locationInfo, resultsInfo;
    private SeekBar radiusSeek;
    private Button btnShowOnMap;

    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    useDeviceLocation();
                } else {
                    toast("Location permission denied – using the fixed location");
                    vm.useFixedLocation();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        modeToggle = view.findViewById(R.id.modeToggle);
        scopeGroup = view.findViewById(R.id.scopeGroup);
        roadPanel = view.findViewById(R.id.roadPanel);
        nearbyPanel = view.findViewById(R.id.nearbyPanel);
        roadInput = view.findViewById(R.id.roadInput);
        radiusLabel = view.findViewById(R.id.radiusLabel);
        radiusSeek = view.findViewById(R.id.radiusSeek);
        locationInfo = view.findViewById(R.id.locationInfo);
        resultsInfo = view.findViewById(R.id.resultsInfo);
        btnShowOnMap = view.findViewById(R.id.btnShowResultsOnMap);

        RecyclerView list = view.findViewById(R.id.list);
        adapter = new SituationAdapter(s -> {
            vm.select(s);
            ((MainActivity) requireActivity()).showDetail();
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        restoreControlsFromViewModel();
        wireListeners(view);

        vm.getSearchResults().observe(getViewLifecycleOwner(), hits -> {
            // Scroll back to the top only when the search itself changed (new road, radius, scope or location),
            // not when the same search is refreshed or the user comes back from a detail screen.
            String key = currentSearchKey();
            boolean newSearch = lastSearchKey != null && !key.equals(lastSearchKey);
            lastSearchKey = key;
            adapter.submitList(hits, () -> {
                if (newSearch) list.scrollToPosition(0);
            });
            updateResultsInfo(hits);
        });
        vm.recomputeSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.nav_search);
    }

    // ------------------------------------------------------------------ controls

    private void restoreControlsFromViewModel() {
        binding = true;
        modeToggle.check(vm.getSearchMode() == SearchMode.ROAD ? R.id.btnModeRoad : R.id.btnModeNearby);
        switch (vm.getScope()) {
            case INCIDENTS: scopeGroup.check(R.id.chipScopeIncidents); break;
            case ROADWORKS: scopeGroup.check(R.id.chipScopeRoadworks); break;
            default:        scopeGroup.check(R.id.chipScopeAll); break;
        }
        if (!vm.getRoadQuery().contentEquals(String.valueOf(roadInput.getText()))) {
            roadInput.setText(vm.getRoadQuery());
        }
        radiusSeek.setProgress(vm.getRadiusKm() - 1);
        updateRadiusLabel(vm.getRadiusKm());
        updatePanels();
        updateLocationInfo();
        binding = false;
    }

    private void wireListeners(View root) {
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (binding || !isChecked) return;
            vm.setSearchMode(checkedId == R.id.btnModeRoad ? SearchMode.ROAD : SearchMode.NEARBY);
            updatePanels();
        });

        scopeGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (binding || ids.isEmpty()) return;
            int id = ids.get(0);
            vm.setScope(id == R.id.chipScopeIncidents ? Scope.INCIDENTS
                    : id == R.id.chipScopeRoadworks ? Scope.ROADWORKS : Scope.ALL);
        });

        roadInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!binding) vm.setRoadQuery(s.toString());
            }
        });
        roadInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        radiusSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                updateRadiusLabel(progress + 1);
                if (fromUser && !binding) vm.setRadiusKm(progress + 1);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        root.findViewById(R.id.btnUseMyLocation).setOnClickListener(v -> requestDeviceLocation());
        root.findViewById(R.id.btnUseFixed).setOnClickListener(v -> {
            vm.useFixedLocation();
            updateLocationInfo();
        });

        btnShowOnMap.setOnClickListener(v -> {
            List<SearchHit> hits = vm.getSearchResults().getValue();
            if (hits == null || hits.isEmpty()) return;
            List<Situation> situations = new ArrayList<>();
            for (SearchHit h : hits) situations.add(h.situation);
            String label = vm.getSearchMode() == SearchMode.ROAD
                    ? "Search results for " + vm.getRoadQuery().trim().toUpperCase(Locale.ROOT)
                    : "Within " + vm.getRadiusKm() + " km";
            ((MainActivity) requireActivity()).openMapWith(situations, label);
        });
    }

    private String currentSearchKey() {
        return vm.getSearchMode() + "|" + vm.getScope() + "|"
                + (vm.getSearchMode() == SearchMode.ROAD
                    ? vm.getRoadQuery().trim().toUpperCase(Locale.ROOT)
                    : vm.getRadiusKm() + "@" + vm.getUserLat() + "," + vm.getUserLon());
    }

    private void updatePanels() {
        boolean road = vm.getSearchMode() == SearchMode.ROAD;
        roadPanel.setVisibility(road ? View.VISIBLE : View.GONE);
        nearbyPanel.setVisibility(road ? View.GONE : View.VISIBLE);
    }

    private void updateRadiusLabel(int km) {
        radiusLabel.setText(String.format(Locale.UK, "Search radius: %d km", km));
    }

    private void updateLocationInfo() {
        locationInfo.setText(String.format(Locale.UK, vm.isUsingDeviceLocation()
                        ? "Using your approximate location (%.3f, %.3f)"
                        : "Using fixed location – Glasgow Caledonian University (%.3f, %.3f)",
                vm.getUserLat(), vm.getUserLon()));
    }

    private void updateResultsInfo(List<SearchHit> hits) {
        int n = hits.size();
        if (vm.getSearchMode() == SearchMode.ROAD) {
            String q = vm.getRoadQuery().trim();
            if (q.isEmpty()) {
                resultsInfo.setText(R.string.search_prompt_road);
            } else if (n == 0) {
                resultsInfo.setText("Nothing found for \"" + q + "\"");
            } else {
                resultsInfo.setText(n + (n == 1 ? " situation" : " situations") + " for \"" + q + "\"");
            }
        } else {
            resultsInfo.setText(n == 0
                    ? "Nothing within " + vm.getRadiusKm() + " km"
                    : n + (n == 1 ? " situation" : " situations") + " within " + vm.getRadiusKm() + " km, nearest first");
            updateLocationInfo();
        }
        btnShowOnMap.setEnabled(n > 0);
    }

    // ------------------------------------------------------------------ location

    private void requestDeviceLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            useDeviceLocation();
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    /** Uses the last known coarse (network / passive) location; falls back to the fixed position. */
    private void useDeviceLocation() {
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        try {
            for (String provider : lm.getProviders(true)) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) continue;   // needs the fine-location permission
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
        } catch (SecurityException e) {
            best = null;
        }
        if (best != null) {
            vm.setLocation(best.getLatitude(), best.getLongitude(), true);
        } else {
            toast("No device location available – using the fixed location");
            vm.useFixedLocation();
        }
        updateLocationInfo();
    }

    private void toast(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }
}
