package com.buddyapps.apphub;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente de la API pública de GitHub con caché y soporte de token opcional. */
public final class GitHubApi {
    public interface Callback<T> {
        void onResult(T value, Throwable error);
    }

    public static final class ApiException extends Exception {
        public ApiException(String message) { super(message); }
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private static SharedPreferences cache;

    private GitHubApi() { }

    public static void init(Context c) {
        if (cache == null) {
            cache = c.getApplicationContext().getSharedPreferences("apphub_cache", 0);
        }
    }

    public static void search(String query, boolean force, Callback<List<AppInfo>> cb) {
        search(query, 1, force, cb);
    }

    public static void search(String query, final int page, boolean force, Callback<List<AppInfo>> cb) {
        final String url = "https://api.github.com/search/repositories?q="
                + encode(query) + "&sort=stars&per_page=12&page=" + page;
        run(new Callable<List<AppInfo>>() {
            @Override public List<AppInfo> call() throws Exception {
                JSONObject root = new JSONObject(fetch(url, true, force));
                JSONArray items = root.optJSONArray("items");
                List<AppInfo> out = new ArrayList<AppInfo>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        out.add(parseRepo(items.optJSONObject(i)));
                    }
                }
                return out;
            }
        }, cb);
    }

    public static void userRepos(boolean force, Callback<List<AppInfo>> cb) {
        final String url = "https://api.github.com/user/repos?per_page=50&sort=updated&affiliation=owner";
        run(new Callable<List<AppInfo>>() {
            @Override public List<AppInfo> call() throws Exception {
                if (!StorePrefs.hasToken()) throw new ApiException("Añade un token de GitHub para ver tus repositorios.");
                JSONArray root = new JSONArray(fetch(url, true, force));
                List<AppInfo> out = new ArrayList<AppInfo>();
                for (int i = 0; i < root.length(); i++) out.add(parseRepo(root.optJSONObject(i)));
                Collections.sort(out, new Comparator<AppInfo>() {
                    @Override public int compare(AppInfo a, AppInfo b) {
                        return a.displayName().compareToIgnoreCase(b.displayName());
                    }
                });
                return out;
            }
        }, cb);
    }

    /** Repositorios públicos de un desarrollador, sin necesidad de token. */
    public static void userPublicRepos(final String login, boolean force, Callback<List<AppInfo>> cb) {
        final String url = "https://api.github.com/users/" + encode(login)
                + "/repos?per_page=40&sort=updated";
        run(new Callable<List<AppInfo>>() {
            @Override public List<AppInfo> call() throws Exception {
                JSONArray root = new JSONArray(fetch(url, true, force));
                List<AppInfo> out = new ArrayList<AppInfo>();
                for (int i = 0; i < root.length(); i++) out.add(parseRepo(root.optJSONObject(i)));
                return out;
            }
        }, cb);
    }

    public static void latestRelease(String repo, boolean force, Callback<ReleaseInfo> cb) {
        final String url = "https://api.github.com/repos/" + repo + "/releases/latest";
        run(new Callable<ReleaseInfo>() {
            @Override public ReleaseInfo call() throws Exception {
                JSONObject o = new JSONObject(fetch(url, true, force));
                String tag = o.optString("tag_name");
                if (tag == null || tag.length() == 0) tag = "";
                String published = o.optString("published_at");
                JSONArray assets = o.optJSONArray("assets");
                final String[] abis = Build.SUPPORTED_ABIS;
                List<JSONObject> apks = new ArrayList<JSONObject>();
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.optJSONObject(i);
                        if (a == null) continue;
                        String n = a.optString("name").toLowerCase(Locale.US);
                        if (n.endsWith(".apk")) apks.add(a);
                    }
                }
                if (apks.isEmpty()) {
                    return new ReleaseInfo(tag, published, null, null, 0, false);
                }
                Collections.sort(apks, new Comparator<JSONObject>() {
                    @Override public int compare(JSONObject a, JSONObject b) {
                        int r = rank(a.optString("name"), abis) - rank(b.optString("name"), abis);
                        if (r != 0) return r;
                        long sa = a.optLong("size"), sb = b.optLong("size");
                        return sa < sb ? -1 : (sa > sb ? 1 : 0);
                    }
                });
                JSONObject best = apks.get(0);
                return new ReleaseInfo(tag, published, best.optString("name"),
                        best.optString("browser_download_url"), best.optLong("size"), true);
            }
        }, cb);
    }

    /** Imágenes y texto de presentación del README, para la ficha de la app. */
    public static void readme(String repo, boolean force, Callback<ReadmeInfo> cb) {
        final String url = readmeUrl(repo);
        run(new Callable<ReadmeInfo>() {
            @Override public ReadmeInfo call() throws Exception {
                return parseReadme(fetch(url, true, force), repo);
            }
        }, cb);
    }

    /** Solo lee caché: sirve para marcar en la lista qué apps tienen capturas sin gastar peticiones. */
    public static ReadmeInfo cachedReadme(String repo) {
        if (cache == null || repo == null) return null;
        try {
            String body = cache.getString(readmeUrl(repo) + "|auth" + ".body", null);
            return body == null ? null : parseReadme(body, repo);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readmeUrl(String repo) {
        return "https://api.github.com/repos/" + repo + "/readme";
    }

    private static ReadmeInfo parseReadme(String body, String repo) throws Exception {
        JSONObject o = new JSONObject(body);
        String encoded = o.optString("content", "").replaceAll("\\s", "");
        String markdown = encoded.length() == 0 ? "" : new String(Base64.decode(encoded, Base64.DEFAULT), "UTF-8");
        return new ReadmeInfo(extractImages(markdown, repo), extractIntro(markdown));
    }

    private static List<String> extractImages(String markdown, String repo) {
        List<String> out = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        Matcher md = Pattern.compile("!\\[[^\\]]*\\]\\(\\s*([^\\s\\)]+)[^\\)]*\\)")
                .matcher(markdown);
        while (md.find()) addImage(out, seen, resolveImage(md.group(1), repo));
        Matcher html = Pattern.compile("<img[^>]*?src\\s*=\\s*[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(markdown);
        while (html.find()) addImage(out, seen, resolveImage(html.group(1), repo));
        return out;
    }

    private static void addImage(List<String> out, Set<String> seen, String url) {
        if (url == null || seen.contains(url)) return;
        String lower = url.toLowerCase(Locale.US);
        if (lower.contains("shields.io") || lower.contains("badge") || lower.contains("travis-ci")
                || lower.contains("codecov") || lower.contains("svg") || lower.startsWith("data:")) return;
        boolean image = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.endsWith(".gif")
                || lower.contains("user-images.githubusercontent.com");
        if (!image) return;
        seen.add(url);
        out.add(url);
        if (out.size() >= 8) return;
    }

    private static String resolveImage(String raw, String repo) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("^<|>$", "");
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        while (s.startsWith("./") || s.startsWith("../")) s = s.replaceFirst("^\\.\\.?/", "");
        if (s.startsWith("/")) s = s.substring(1);
        return "https://raw.githubusercontent.com/" + repo + "/HEAD/" + s;
    }

    private static String extractIntro(String markdown) {
        String clean = markdown.replaceAll("(?s)```.*?```", "");
        String[] lines = clean.split("\\n");
        StringBuilder out = new StringBuilder();
        Pattern link = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)");
        for (String line : lines) {
            String s = line.trim();
            if (s.length() < 40 || s.startsWith("#") || s.startsWith("!") || s.startsWith("<")
                    || s.startsWith("|") || s.startsWith("---") || s.startsWith("[![")
                    || s.toLowerCase(Locale.US).contains("shields.io")) continue;
            Matcher m = link.matcher(s);
            s = m.replaceAll("$1");
            s = s.replaceAll("<[^>]+>", "").replace("**", "").replace("__", "")
                    .replace("`", "").replace("*", "");
            s = s.trim();
            if (s.length() < 40) continue;
            if (out.length() > 0) out.append(' ');
            out.append(s);
            if (out.length() > 350) break;
        }
        if (out.length() > 360) return out.substring(0, 360) + "…";
        return out.toString();
    }

    private static int rank(String name, String[] abis) {
        String n = name.toLowerCase(Locale.US);
        if (n.contains("universal")) return 0;
        for (int i = 0; i < abis.length; i++) {
            String abi = abis[i].toLowerCase(Locale.US);
            String shortAbi = abi.contains("-") ? abi.substring(0, abi.indexOf('-')) : abi;
            if (n.contains(abi) || n.contains(shortAbi)) return i + 1;
        }
        return 50;
    }

    private static AppInfo parseRepo(JSONObject o) {
        if (o == null) return null;
        JSONObject owner = o.optJSONObject("owner");
        String login = owner == null ? "" : owner.optString("login");
        String avatar = owner == null ? "" : owner.optString("avatar_url");
        List<String> topics = new ArrayList<String>();
        JSONArray topicArray = o.optJSONArray("topics");
        if (topicArray != null) {
            for (int i = 0; i < topicArray.length(); i++) topics.add(topicArray.optString(i));
        }
        AppInfo app = new AppInfo(o.optString("full_name"), login, o.optString("name"),
                o.optString("description"), o.optString("html_url"), avatar,
                o.optInt("stargazers_count"), topics);
        app.category = Categories.classify(app);
        return app;
    }

    private static <T> void run(final Callable<T> job, final Callback<T> cb) {
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    final T value = job.call();
                    MAIN.post(new Runnable() {
                        @Override public void run() { cb.onResult(value, null); }
                    });
                } catch (final Throwable error) {
                    MAIN.post(new Runnable() {
                        @Override public void run() { cb.onResult(null, error); }
                    });
                }
            }
        });
    }

    private static String fetch(String url, boolean useToken, boolean force) throws Exception {
        String key = url + (useToken ? "|auth" : "|public");
        long now = System.currentTimeMillis();
        long ttl = url.contains("/search/") ? 60_000L : 10 * 60_000L;
        String cachedBody = cache.getString(key + ".body", null);
        String etag = cache.getString(key + ".etag", null);
        if (!force && cachedBody != null && now - cache.getLong(key + ".ts", 0) < ttl) {
            return cachedBody;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "AppHub/1.0 (Android)");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            if (useToken && StorePrefs.hasToken()) {
                conn.setRequestProperty("Authorization", "Bearer " + StorePrefs.token());
            }
            if (!force && etag != null && cachedBody != null) {
                conn.setRequestProperty("If-None-Match", etag);
            }
            int code = conn.getResponseCode();
            if (code == 304 && cachedBody != null) {
                cache.edit().putLong(key + ".ts", System.currentTimeMillis()).apply();
                return cachedBody;
            }
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = read(in);
            if (code >= 400) throw new ApiException(friendly(code, body));
            String newEtag = conn.getHeaderField("ETag");
            SharedPreferences.Editor e = cache.edit();
            e.putString(key + ".body", body);
            e.putLong(key + ".ts", System.currentTimeMillis());
            if (newEtag != null) e.putString(key + ".etag", newEtag);
            e.apply();
            return body;
        } catch (IOException e) {
            throw new ApiException("No se pudo conectar con GitHub. Comprueba tu conexión e inténtalo otra vez.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String friendly(int code, String body) {
        String msg = "";
        try {
            JSONObject o = new JSONObject(body);
            msg = o.optString("message", "");
        } catch (Exception ignored) { }
        String low = msg.toLowerCase(Locale.US);
        if (code == 401) return "El token de GitHub no es válido o ha caducado.";
        if (code == 403 && low.contains("rate limit")) {
            return "GitHub ha alcanzado el límite de peticiones. Vuelve a intentarlo en unos minutos o conecta tu token.";
        }
        if (code == 403) return "GitHub ha denegado la petición. " + msg;
        if (code == 404) return "No se encontró esa página o ese repositorio en GitHub.";
        if (code >= 500) return "GitHub no está respondiendo ahora. Inténtalo de nuevo en unos minutos.";
        return msg.length() > 0 ? msg : "Error " + code + " al consultar GitHub.";
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    private static String encode(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
