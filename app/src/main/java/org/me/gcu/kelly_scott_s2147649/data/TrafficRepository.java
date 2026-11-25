package org.me.gcu.kelly_scott_s2147649.data;

import android.content.Context;
import android.util.Log;

import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The "Model" of the app: downloads a feed, parses it with {@link TrafficXmlParser} and keeps a copy of the
 * last good download on disk, so that something useful can still be shown when there is no connection.
 *
 * All methods block, so they must be called from a background thread (see TrafficViewModel).
 */
public class TrafficRepository {
    private static final String TAG = "TrafficRepository";
    /** Error text used when the device could not reach the server at all. */
    public static final String NO_CONNECTION = "No connection";
    private static final String BAD_CREDENTIALS =
            "Traffic Scotland rejected the client ID/key – check local.properties";

    private final File cacheDir;
    private final Credentials credentials;

    public TrafficRepository(Context context) {
        this.cacheDir = context.getFilesDir();
        this.credentials = new Credentials(context);
    }

    public boolean hasCredentials() {
        return credentials.looksConfigured();
    }

    /** Downloads, parses and caches one feed; falls back to the cached copy if anything goes wrong. */
    public FeedResult loadFeed(SituationKind kind) {
        String url = kind == SituationKind.INCIDENT
                ? FeedConfig.UNPLANNED_EVENTS_URL : FeedConfig.CURRENT_ROADWORKS_URL;
        File cacheFile = new File(cacheDir, "feed_" + kind.name().toLowerCase() + ".xml");

        String problem;
        try {
            String xml = download(url);
            List<Situation> parsed = TrafficXmlParser.parse(xml, kind);   // parse BEFORE caching: never cache junk
            long now = System.currentTimeMillis();
            writeCache(cacheFile, xml);
            Log.d(TAG, kind + ": downloaded and parsed " + parsed.size() + " situations");
            return FeedResult.fresh(parsed, now);
        } catch (Exception e) {
            problem = describe(e);
            Log.w(TAG, kind + ": load failed (" + problem + "), trying cache");
        }

        if (cacheFile.exists()) {
            try {
                List<Situation> parsed = TrafficXmlParser.parse(readCache(cacheFile), kind);
                return FeedResult.cached(parsed, cacheFile.lastModified(), problem);
            } catch (Exception e) {
                Log.e(TAG, kind + ": cache unreadable", e);
            }
        }
        return FeedResult.failed(problem);
    }

    // ---------------------------------------------------------------- network

    private String download(String address) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        try {
            conn.setConnectTimeout(FeedConfig.CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(FeedConfig.READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", credentials.authorizationHeader());
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new IOException(BAD_CREDENTIALS + " (HTTP " + code + ")");
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server answered HTTP " + code);
            }
            StringBuilder sb = new StringBuilder(256 * 1024);   // StringBuilder: the roadworks feed is >1 MB
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int n;
                while ((n = in.read(buf)) != -1) sb.append(buf, 0, n);
            }
            // With a wrong client ID/key the server still answers HTTP 200, but with its HTML login page
            // instead of the XML feed. Spot that here so the user gets a clear message, not a parser error.
            if (looksLikeHtml(sb)) {
                throw new IOException(BAD_CREDENTIALS);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    /** True if the start of the response is an HTML page rather than the DATEX II XML feed. */
    private static boolean looksLikeHtml(CharSequence body) {
        String start = body.subSequence(0, Math.min(body.length(), 1024)).toString().toLowerCase(java.util.Locale.ROOT);
        return start.contains("<html") || start.contains("<!doctype html");
    }

    // ---------------------------------------------------------------- cache

    private void writeCache(File target, String xml) {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Could not write cache", e);
            return;
        }
        if (!tmp.renameTo(target)) {
            tmp.delete();
        }
    }

    private String readCache(File file) throws IOException {
        StringBuilder sb = new StringBuilder((int) Math.min(file.length(), 8_000_000L));
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            char[] buf = new char[8192];
            int n;
            while ((n = in.read(buf)) != -1) sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    private static String describe(Exception e) {
        if (e instanceof java.net.UnknownHostException || e instanceof java.net.ConnectException
                || e instanceof java.net.SocketTimeoutException) {
            return NO_CONNECTION;
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
