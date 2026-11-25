package org.me.gcu.kelly_scott_s2147649.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.Chip;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.RoadPoint;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Google map of all situations (filterable by incidents / roadworks) or of a chosen subset (one situation from the
 * detail screen, or the results of a road / distance search). Linear situations are drawn as a line between
 * their "from" and "to" points, with a category icon at the middle.
 */
public class TrafficMapFragment extends Fragment implements OnMapReadyCallback {
    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private TrafficViewModel vm;
    private GoogleMap map;
    private Chip chipIncidents, chipRoadworks;
    private TextView mapInfo;
    private Button btnShowAll;
    private List<Situation> lastFittedSelection;   // so auto-updates don't keep re-centring the camera

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        chipIncidents = view.findViewById(R.id.chipIncidents);
        chipRoadworks = view.findViewById(R.id.chipRoadworks);
        mapInfo = view.findViewById(R.id.mapInfo);
        btnShowAll = view.findViewById(R.id.btnShowAll);

        chipIncidents.setOnCheckedChangeListener((b, checked) -> redraw());
        chipRoadworks.setOnCheckedChangeListener((b, checked) -> redraw());
        btnShowAll.setOnClickListener(v -> vm.showAllOnMap());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        vm.getIncidents().observe(getViewLifecycleOwner(), l -> redraw());
        vm.getRoadworks().observe(getViewLifecycleOwner(), l -> redraw());
        vm.getMapSelection().observe(getViewLifecycleOwner(), l -> redraw());
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.nav_map);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6.2f));
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) {
                vm.select((Situation) tag);
                ((MainActivity) requireActivity()).showDetail();
            }
        });
        redraw();
    }

    private void redraw() {
        if (map == null || !isAdded()) return;

        List<Situation> selection = vm.getMapSelection().getValue();
        boolean selectionMode = selection != null;
        List<Situation> toShow = new ArrayList<>();
        if (selectionMode) {
            toShow.addAll(selection);
        } else {
            if (chipIncidents.isChecked()) addAll(toShow, vm.getIncidents().getValue());
            if (chipRoadworks.isChecked()) addAll(toShow, vm.getRoadworks().getValue());
        }

        chipIncidents.setEnabled(!selectionMode);
        chipRoadworks.setEnabled(!selectionMode);
        btnShowAll.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

        map.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int points = 0, drawn = 0;
        LatLng single = null;

        for (Situation s : toShow) {
            if (!s.hasLocation()) continue;
            double[] mid = s.getMidpoint();
            LatLng pos = new LatLng(mid[0], mid[1]);

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(s.getKind().getLabel() + ": " + s.getTypeLabel())
                    .snippet(shorten(s.getComment()))
                    .icon(IconHelper.marker(requireContext(), s.getCategory()))
                    .anchor(0.5f, 0.5f));
            if (marker != null) marker.setTag(s);
            drawn++;

            RoadPoint f = s.getFrom(), t = s.getTo();
            if (f != null && t != null && f.hasCoordinates() && t.hasCoordinates()) {
                LatLng a = new LatLng(f.getLatitude(), f.getLongitude());
                LatLng b = new LatLng(t.getLatitude(), t.getLongitude());
                int colour = s.getKind() == SituationKind.ROADWORK
                        ? IconHelper.delayColor(requireContext(), s.getDelay())
                        : IconHelper.categoryColor(requireContext(), s.getCategory());
                map.addPolyline(new PolylineOptions().add(a, b).width(9f).color(colour));
                bounds.include(a);
                bounds.include(b);
                points += 2;
            } else {
                bounds.include(pos);
                points++;
            }
            single = pos;
        }

        // camera: only move it when a (new) selection is being shown
        if (selectionMode && selection != lastFittedSelection && points > 0) {
            lastFittedSelection = selection;
            fitCamera(bounds, points, single);
        } else if (!selectionMode) {
            lastFittedSelection = null;
        }

        if (selectionMode) {
            mapInfo.setText(vm.getMapSelectionLabel().getValue() + " – " + drawn + " on map");
        } else {
            mapInfo.setText(drawn + " situations on map – tap an icon for details");
        }
    }

    private void fitCamera(LatLngBounds.Builder bounds, int points, LatLng single) {
        try {
            if (points == 1 && single != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(single, 12f));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 120));
            }
        } catch (IllegalStateException e) {
            // map not laid out yet: fall back to a fixed zoom around the last point
            if (single != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(single, 10f));
        }
    }

    private static void addAll(List<Situation> target, @Nullable List<Situation> source) {
        if (source != null) target.addAll(source);
    }

    private static String shorten(String s) {
        return s.length() <= 110 ? s : s.substring(0, 107) + "…";
    }
}
