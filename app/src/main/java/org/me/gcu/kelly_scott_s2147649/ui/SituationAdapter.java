package org.me.gcu.kelly_scott_s2147649.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.DelayLevel;
import org.me.gcu.kelly_scott_s2147649.model.SearchHit;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** One row per situation: icon, type, description (generalPublicComment) and a small info line. */
public class SituationAdapter extends ListAdapter<SearchHit, SituationAdapter.Holder> {

    public interface Listener {
        void onSituationClicked(Situation situation);
    }

    private final Listener listener;

    public SituationAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<SearchHit>() {
            @Override
            public boolean areItemsTheSame(@NonNull SearchHit a, @NonNull SearchHit b) {
                return a.situation.getKind() == b.situation.getKind()
                        && a.situation.getId().equals(b.situation.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull SearchHit a, @NonNull SearchHit b) {
                return Objects.equals(a.situation.getVersionTime(), b.situation.getVersionTime())
                        && a.situation.getComment().equals(b.situation.getComment())
                        && a.situation.getDelay() == b.situation.getDelay()
                        && Double.compare(a.distanceKm, b.distanceKm) == 0;
            }
        });
        this.listener = listener;
    }

    /** Wraps plain situations as hits without a distance. */
    public static List<SearchHit> wrap(List<Situation> situations) {
        List<SearchHit> hits = new ArrayList<>(situations.size());
        for (Situation s : situations) hits.add(new SearchHit(s, Double.NaN));
        return hits;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        SearchHit hit = getItem(position);
        Situation s = hit.situation;
        Context ctx = h.itemView.getContext();

        h.icon.setImageResource(IconHelper.iconRes(s.getCategory()));
        h.title.setText(s.getKind() == SituationKind.INCIDENT ? "Incident · " + s.getTypeLabel() : s.getTypeLabel());
        h.comment.setText(s.getComment());

        // colour coded delay stripe for roadworks
        if (s.getKind() == SituationKind.ROADWORK) {
            h.stripe.setBackgroundColor(IconHelper.delayColor(ctx, s.getDelay()));
        } else {
            h.stripe.setBackgroundColor(Color.TRANSPARENT);
        }

        StringBuilder meta = new StringBuilder();
        if (hit.hasDistance()) meta.append(String.format(Locale.UK, "%.1f km away", hit.distanceKm));
        if (s.getKind() == SituationKind.ROADWORK) {
            if (s.getDelay() != DelayLevel.NONE) append(meta, s.getDelay().getLabel());
            if (s.getEnd() != null) append(meta, "until " + DetailBinder.formatDate(s.getEnd()));
        } else if (s.getStart() != null) {
            append(meta, "since " + DetailBinder.formatDateTime(s.getStart()));
        }
        h.meta.setVisibility(meta.length() == 0 ? View.GONE : View.VISIBLE);
        h.meta.setText(meta);

        h.itemView.setOnClickListener(v -> listener.onSituationClicked(s));
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(part);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View stripe;
        final ImageView icon;
        final TextView title, comment, meta;

        Holder(@NonNull View v) {
            super(v);
            stripe = v.findViewById(R.id.stripe);
            icon = v.findViewById(R.id.icon);
            title = v.findViewById(R.id.title);
            comment = v.findViewById(R.id.comment);
            meta = v.findViewById(R.id.meta);
        }
    }
}
