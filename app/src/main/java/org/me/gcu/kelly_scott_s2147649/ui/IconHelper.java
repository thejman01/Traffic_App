package org.me.gcu.kelly_scott_s2147649.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.me.gcu.kelly_scott_s2147649.R;
import org.me.gcu.kelly_scott_s2147649.model.DelayLevel;
import org.me.gcu.kelly_scott_s2147649.model.SituationCategory;

import java.util.EnumMap;
import java.util.Map;

/** Maps categories / delay levels to icons and colours (single place, used by lists, details and the map). */
public final class IconHelper {
    private static final Map<SituationCategory, BitmapDescriptor> MARKER_CACHE = new EnumMap<>(SituationCategory.class);

    private IconHelper() {}

    @DrawableRes
    public static int iconRes(SituationCategory c) {
        switch (c) {
            case ROAD_CLOSED:        return R.drawable.ic_cat_road_closed;
            case LANE_RESTRICTION:   return R.drawable.ic_cat_lane_restriction;
            case QUEUING_TRAFFIC:    return R.drawable.ic_cat_queuing;
            case SLOW_TRAFFIC:       return R.drawable.ic_cat_slow_traffic;
            case ROADWORKS:          return R.drawable.ic_cat_roadworks;
            case ROADWORKS_CLOSURE:  return R.drawable.ic_cat_roadworks_closure;
            default:                 return R.drawable.ic_cat_other;
        }
    }

    public static int categoryColor(Context ctx, SituationCategory c) {
        int res;
        switch (c) {
            case ROAD_CLOSED:        res = R.color.cat_closed; break;
            case LANE_RESTRICTION:   res = R.color.cat_lane; break;
            case QUEUING_TRAFFIC:    res = R.color.cat_queue; break;
            case SLOW_TRAFFIC:       res = R.color.cat_slow; break;
            case ROADWORKS:          res = R.color.cat_roadworks; break;
            case ROADWORKS_CLOSURE:  res = R.color.cat_roadworks_closure; break;
            default:                 res = R.color.cat_other; break;
        }
        return ContextCompat.getColor(ctx, res);
    }

    /** Colour coding of roadworks delays: grey (unknown) / yellow / orange / red. */
    public static int delayColor(Context ctx, DelayLevel d) {
        int res;
        switch (d) {
            case DELAYS:           res = R.color.delay_short; break;
            case LONG_DELAYS:      res = R.color.delay_long; break;
            case VERY_LONG_DELAYS: res = R.color.delay_very_long; break;
            default:               res = R.color.delay_none; break;
        }
        return ContextCompat.getColor(ctx, res);
    }

    /** Map marker for a category (the vector icon rendered once to a bitmap, then cached). */
    public static BitmapDescriptor marker(Context ctx, SituationCategory c) {
        BitmapDescriptor cached = MARKER_CACHE.get(c);
        if (cached != null) return cached;
        Drawable d = ContextCompat.getDrawable(ctx, iconRes(c));
        int size = Math.round(30 * ctx.getResources().getDisplayMetrics().density);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(canvas);
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bmp);
        MARKER_CACHE.put(c, descriptor);
        return descriptor;
    }
}
