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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.me.gcu.kelly_scott_s2147649.viewmodel.LoadStatus;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel;

import java.util.Collections;
import java.util.List;

/**
 * List of all incidents or all roadworks (type + generalPublicComment per row).
 *
 * Portrait: tapping a row opens the detail screen. Landscape (layout-land has a "detailPane"):
 * master/detail, the details of the tapped row appear on the right without leaving the screen.
 */
public class SituationListFragment extends Fragment {
    private static final String ARG_KIND = "kind";

    private TrafficViewModel viewModel;
    private SituationKind kind;
    private SituationAdapter adapter;
    private TextView emptyView;
    private View detailPane, detailPlaceholder, detailScroll;
    private List<Situation> currentList = Collections.emptyList();

    public static SituationListFragment newInstance(SituationKind kind) {
        SituationListFragment f = new SituationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KIND, kind.name());
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        kind = SituationKind.valueOf(requireArguments().getString(ARG_KIND, SituationKind.INCIDENT.name()));
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        RecyclerView list = view.findViewById(R.id.list);
        emptyView = view.findViewById(R.id.emptyView);
        detailPane = view.findViewById(R.id.detailPane);               // only exists in the landscape layout
        detailPlaceholder = view.findViewById(R.id.detailPlaceholder);
        detailScroll = view.findViewById(R.id.detailScroll);
        view.findViewById(R.id.delayLegend).setVisibility(kind == SituationKind.ROADWORK ? View.VISIBLE : View.GONE);

        adapter = new SituationAdapter(this::onSituationClicked);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        (kind == SituationKind.INCIDENT ? viewModel.getIncidents() : viewModel.getRoadworks())
                .observe(getViewLifecycleOwner(), situations -> {
                    currentList = situations;
                    adapter.submitList(SituationAdapter.wrap(situations));
                    updateEmptyView(viewModel.getStatus().getValue());
                });
        viewModel.getStatus().observe(getViewLifecycleOwner(), this::updateEmptyView);

        if (detailPane != null) {
            viewModel.getSelected().observe(getViewLifecycleOwner(), this::bindDetailPane);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(kind == SituationKind.INCIDENT ? R.string.nav_incidents : R.string.nav_roadworks);
    }

    private void onSituationClicked(Situation s) {
        viewModel.select(s);
        if (detailPane == null) {
            ((MainActivity) requireActivity()).showDetail();          // portrait: separate screen
        }                                                             // landscape: the observer fills the pane
    }

    private void updateEmptyView(@Nullable LoadStatus status) {
        if (emptyView == null) return;
        if (!currentList.isEmpty()) {
            emptyView.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.VISIBLE);
        if (status == null || status.state == LoadStatus.State.LOADING) {
            emptyView.setText("Loading…");
        } else if (status.state == LoadStatus.State.ERROR_NO_DATA) {
            emptyView.setText(status.message);
        } else {
            emptyView.setText(R.string.empty_list);
        }
    }

    private void bindDetailPane(@Nullable Situation s) {
        boolean show = s != null && s.getKind() == kind;
        detailPlaceholder.setVisibility(show ? View.GONE : View.VISIBLE);
        detailScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        DetailBinder.bind(detailScroll, s);
        Button mapButton = detailScroll.findViewById(R.id.detailMapButton);
        mapButton.setOnClickListener(v -> ((MainActivity) requireActivity())
                .openMapWith(Collections.singletonList(s), s.getTypeLabel()));
    }
}
