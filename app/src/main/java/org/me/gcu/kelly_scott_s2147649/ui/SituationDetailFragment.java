package org.me.gcu.kelly_scott_s2147649.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.viewmodel.TrafficViewModel;

import java.util.Collections;

/** Full-screen details of the selected situation (portrait: one column, landscape: two columns). */
public class SituationDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_situation_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TrafficViewModel vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        vm.getSelected().observe(getViewLifecycleOwner(), s -> {
            if (s == null) {
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }
            bind(view, s);
        });
    }

    private void bind(View view, Situation s) {
        DetailBinder.bind(view, s);
        Button mapButton = view.findViewById(R.id.detailMapButton);
        mapButton.setOnClickListener(v -> ((MainActivity) requireActivity())
                .openMapWith(Collections.singletonList(s), s.getTypeLabel()));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Details");
    }
}
