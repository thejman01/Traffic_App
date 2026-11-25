package org.me.gcu.kelly_scott_s2147649.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.RoadPoint;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.me.gcu.kelly_scott_s2147649.util.TextUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Fills the detail views (shared by the detail screen and the landscape detail pane) with everything we
 * extracted from the XML for one situation. Expects the view ids detailIcon, detailType, detailKind,
 * detailDelay, detailComment and detailFacts somewhere below {@code root}.
 */
public final class DetailBinder {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);

    private DetailBinder() {}

    public static String formatDateTime(OffsetDateTime t) {
        return t == null ? "" : t.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_TIME);
    }

    public static String formatDate(OffsetDateTime t) {
        return t == null ? "" : t.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_ONLY);
    }

    public static void bind(View root, Situation s) {
        Context ctx = root.getContext();

        ((ImageView) root.findViewById(R.id.detailIcon)).setImageResource(IconHelper.iconRes(s.getCategory()));
        ((TextView) root.findViewById(R.id.detailType)).setText(s.getTypeLabel());
        ((TextView) root.findViewById(R.id.detailKind)).setText(
                s.getKind().getLabel() + " · " + s.getCategory().getLabel());
        ((TextView) root.findViewById(R.id.detailComment)).setText(s.getComment());

        // colour-coded delay chip (roadworks)
        TextView delay = root.findViewById(R.id.detailDelay);
        if (s.getKind() == SituationKind.ROADWORK) {
            delay.setVisibility(View.VISIBLE);
            delay.setText(s.getDelay().getLabel());
            delay.getBackground().mutate().setTint(IconHelper.delayColor(ctx, s.getDelay()));
            // dark text on yellow/grey, white on orange/red
            boolean dark = s.getDelay().getSeverity() <= 1;
            delay.setTextColor(dark ? 0xFF000000 : 0xFFFFFFFF);
        } else {
            delay.setVisibility(View.GONE);
        }

        LinearLayout facts = root.findViewById(R.id.detailFacts);
        facts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        header(inflater, facts, "Overview");
        row(inflater, facts, "Type", s.getTypeLabel());
        row(inflater, facts, "Reference", s.getRecordId().isEmpty() ? s.getId() : s.getRecordId());
        row(inflater, facts, "Status", join(", ", TextUtils.humanise(s.getInformationStatus()),
                TextUtils.humanise(s.getProbability())));
        row(inflater, facts, "Source", s.getSource());
        if (s.isUrgent()) row(inflater, facts, "Urgent works", "Yes");

        header(inflater, facts, "Impact");
        if (s.getLanesRestricted() != null) row(inflater, facts, "Lanes restricted", String.valueOf(s.getLanesRestricted()));
        if (s.getLanesOperational() != null) {
            int n = s.getLanesOperational();
            row(inflater, facts, "Lanes open", n == 0 ? "0 (closed)" : String.valueOf(n));
        }
        if (s.getKind() == SituationKind.ROADWORK) row(inflater, facts, "Delays", s.getDelay().getLabel());
        row(inflater, facts, "Compliance", TextUtils.humanise(s.getCompliance()));

        header(inflater, facts, "Validity");
        row(inflater, facts, "Starts", formatDateTime(s.getStart()));
        row(inflater, facts, "Ends", s.getEnd() != null ? formatDateTime(s.getEnd())
                : (s.getKind() == SituationKind.INCIDENT ? "Until further notice" : "Not given"));
        row(inflater, facts, "Planned length", plannedLength(s));
        row(inflater, facts, "Last updated", formatDateTime(s.getVersionTime()));
        row(inflater, facts, "First reported", formatDateTime(s.getCreated()));
        row(inflater, facts, "Validity", TextUtils.humanise(s.getValidityStatus()));

        header(inflater, facts, "Location");
        row(inflater, facts, "Roads", String.join(", ", s.getRoadNames()));
        row(inflater, facts, "Direction", TextUtils.humanise(s.getDirection()));
        row(inflater, facts, "From", describe(s.getFrom()));
        row(inflater, facts, "To", describe(s.getTo()));
    }

    private static String plannedLength(Situation s) {
        if (s.getStart() == null || s.getEnd() == null) return "";
        Duration d = Duration.between(s.getStart(), s.getEnd());
        long days = d.toDays();
        if (days >= 1) return days + (days == 1 ? " day" : " days");
        long hours = Math.max(1, d.toHours());
        return hours + (hours == 1 ? " hour" : " hours");
    }

    private static String describe(RoadPoint p) {
        if (p == null) return "";
        String name = p.getDisplayName();
        String coords = p.hasCoordinates()
                ? String.format(Locale.UK, "%.5f, %.5f", p.getLatitude(), p.getLongitude()) : "";
        return join("\n", name, coords);
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static void header(LayoutInflater inflater, LinearLayout parent, String text) {
        TextView tv = (TextView) inflater.inflate(R.layout.item_fact_header, parent, false);
        tv.setText(text);
        parent.addView(tv);
    }

    /** Rows with no data are simply left out. */
    private static void row(LayoutInflater inflater, LinearLayout parent, String label, String value) {
        if (value == null || value.isEmpty()) return;
        View row = inflater.inflate(R.layout.item_fact, parent, false);
        ((TextView) row.findViewById(R.id.factLabel)).setText(label);
        ((TextView) row.findViewById(R.id.factValue)).setText(value);
        parent.addView(row);
    }
}
