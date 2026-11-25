package org.me.gcu.kelly_scott_s2147649.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

/**
 * Reads the Traffic Scotland client id / key from the manifest meta-data. The values are injected at
 * build time from local.properties (see protectingYourKeys), so they never appear in the source code.
 */
public final class Credentials {
    private static final String TAG = "Credentials";
    private final String clientId;
    private final String clientKey;

    public Credentials(Context context) {
        String id = "";
        String key = "";
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle meta = info.metaData;
            if (meta != null) {
                id = meta.getString("org.me.gcu.kelly_scott_s2147649.TS_CLIENT_ID", "");
                key = meta.getString("org.me.gcu.kelly_scott_s2147649.TS_CLIENT_KEY", "");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read manifest meta-data", e);
        }
        clientId = id;
        clientKey = key;
    }

    /** Value for the Authorization header, exactly as in the supplied skeleton code: Base64(clientId:clientKey). */
    public String authorizationHeader() {
        return Base64.encodeToString((clientId + ":" + clientKey).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Base64.NO_WRAP);
    }

    public boolean looksConfigured() {
        return !clientId.isEmpty() && !clientId.startsWith("YOUR_");
    }
}
