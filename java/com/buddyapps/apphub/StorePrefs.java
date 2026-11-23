package com.buddyapps.apphub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Estado local de AppHub: token, repo propio, apps instaladas y descargas pendientes. */
public final class StorePrefs {
    private static final String FILE = "apphub_store";
    private static final String TOKEN = "token";
    private static final String SELF_REPO = "self_repo";
    private static final String APPS = "installed_apps";
    private static final String DOWNLOADS = "pending_downloads";
    private static final String PENDING = "pending_installs";

    private static SharedPreferences prefs;

    private StorePrefs() { }

    public static void init(Context c) {
        if (prefs == null) prefs = c.getApplicationContext().getSharedPreferences(FILE, 0);
    }

    public static String token() {
        String t = prefs.getString(TOKEN, "");
        return (t == null || t.length() == 0) ? null : t;
    }

    public static boolean hasToken() { return token() != null; }

    public static void setToken(String token) {
        if (token == null || token.trim().length() == 0) prefs.edit().remove(TOKEN).apply();
        else prefs.edit().putString(TOKEN, token.trim()).apply();
    }

    public static String selfRepo() {
        if (!prefs.contains(SELF_REPO)) return AppConfig.SELF_REPO;
        String r = prefs.getString(SELF_REPO, "");
        return (r == null || r.length() == 0) ? null : r;
    }

    public static void setSelfRepo(String repo) {
        prefs.edit().putString(SELF_REPO, repo == null ? "" : repo.trim()).apply();
    }

    public static final class Tracked {
        public final String repo;
        public final String version;
        public final String apkName;
        public final String packageName;
        public final String appName;
        public final long installedAt;

        Tracked(String repo, String version, String apkName, String packageName,
                String appName, long installedAt) {
            this.repo = repo;
            this.version = version;
            this.apkName = apkName;
            this.packageName = packageName;
            this.appName = appName;
            this.installedAt = installedAt;
        }
    }

    /** APK descargado esperando a que el sistema confirme su instalación. */
    public static final class PendingInstall {
        public final String packageName, repo, version, apkName, appName;
        PendingInstall(String packageName, String repo, String version, String apkName, String appName) {
            this.packageName = packageName;
            this.repo = repo;
            this.version = version;
            this.apkName = apkName;
            this.appName = appName;
        }
    }

    public static final class DownloadInfo {
        public final long id;
        public final String repo;
        public final String appName;
        public final String version;
        public final String apkName;

        DownloadInfo(long id, String repo, String appName, String version, String apkName) {
            this.id = id;
            this.repo = repo;
            this.appName = appName;
            this.version = version;
            this.apkName = apkName;
        }
    }

    public static synchronized void track(String repo, String version, String apkName) {
        track(repo, version, apkName, "", "");
    }

    public static synchronized void track(String repo, String version, String apkName,
                                          String packageName, String appName) {
        try {
            JSONObject root = new JSONObject(prefs.getString(APPS, "{}"));
            JSONObject item = new JSONObject();
            item.put("version", version == null ? "" : version);
            item.put("apkName", apkName == null ? "" : apkName);
            item.put("packageName", packageName == null ? "" : packageName);
            item.put("appName", appName == null ? "" : appName);
            item.put("installedAt", System.currentTimeMillis());
            root.put(repo, item);
            prefs.edit().putString(APPS, root.toString()).apply();
        } catch (Exception ignored) {
            // Si el estado se corrompe, se descarta y se empieza de cero.
            prefs.edit().remove(APPS).apply();
        }
    }

    public static synchronized Tracked tracked(String repo) {
        try {
            JSONObject root = new JSONObject(prefs.getString(APPS, "{}"));
            JSONObject item = root.optJSONObject(repo);
            if (item == null) return null;
            return new Tracked(repo, item.optString("version"), item.optString("apkName"),
                    item.optString("packageName"), item.optString("appName"),
                    item.optLong("installedAt"));
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized List<Tracked> allTracked() {
        List<Tracked> out = new ArrayList<Tracked>();
        try {
            JSONObject root = new JSONObject(prefs.getString(APPS, "{}"));
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String repo = keys.next();
                JSONObject item = root.optJSONObject(repo);
                if (item != null) out.add(new Tracked(repo, item.optString("version"),
                        item.optString("apkName"), item.optString("packageName"),
                        item.optString("appName"), item.optLong("installedAt")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Busca una app registrada por el nombre de paquete instalado (o por nombre visible). */
    public static synchronized Tracked trackedByPackage(String packageName, String appName) {
        for (Tracked t : allTracked()) {
            if (packageName != null && packageName.length() > 0
                    && packageName.equals(t.packageName)) return t;
            if (appName != null && appName.length() > 0 && appName.equals(t.appName)) return t;
        }
        return null;
    }

    public static synchronized void addPendingInstall(String packageName, String repo,
                                                      String version, String apkName, String appName) {
        if (packageName == null || packageName.length() == 0) return;
        try {
            JSONObject root = new JSONObject(prefs.getString(PENDING, "{}"));
            JSONObject item = new JSONObject();
            item.put("repo", repo == null ? "" : repo);
            item.put("version", version == null ? "" : version);
            item.put("apkName", apkName == null ? "" : apkName);
            item.put("appName", appName == null ? "" : appName);
            root.put(packageName, item);
            prefs.edit().putString(PENDING, root.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static synchronized PendingInstall pendingInstall(String packageName) {
        if (packageName == null || packageName.length() == 0) return null;
        try {
            JSONObject root = new JSONObject(prefs.getString(PENDING, "{}"));
            JSONObject item = root.optJSONObject(packageName);
            if (item == null) return null;
            return new PendingInstall(packageName, item.optString("repo"), item.optString("version"),
                    item.optString("apkName"), item.optString("appName"));
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized void removePendingInstall(String packageName) {
        if (packageName == null) return;
        try {
            JSONObject root = new JSONObject(prefs.getString(PENDING, "{}"));
            root.remove(packageName);
            prefs.edit().putString(PENDING, root.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** True si el paquete corresponde a una app instalada desde AppHub. */
    public static synchronized boolean isFromHub(String packageName) {
        if (packageName == null || packageName.length() == 0) return false;
        for (Tracked t : allTracked()) if (packageName.equals(t.packageName)) return true;
        return false;
    }

    public static synchronized void removeTracked(String repo) {
        try {
            JSONObject root = new JSONObject(prefs.getString(APPS, "{}"));
            root.remove(repo);
            prefs.edit().putString(APPS, root.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static synchronized void addDownload(long id, String repo, String appName,
                                                String version, String apkName) {
        try {
            JSONObject root = new JSONObject(prefs.getString(DOWNLOADS, "{}"));
            JSONObject item = new JSONObject();
            item.put("repo", repo == null ? "" : repo);
            item.put("appName", appName == null ? "" : appName);
            item.put("version", version == null ? "" : version);
            item.put("apkName", apkName == null ? "" : apkName);
            root.put(Long.toString(id), item);
            prefs.edit().putString(DOWNLOADS, root.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static synchronized DownloadInfo getDownload(long id) {
        try {
            JSONObject root = new JSONObject(prefs.getString(DOWNLOADS, "{}"));
            JSONObject item = root.optJSONObject(Long.toString(id));
            if (item == null) return null;
            return new DownloadInfo(id, item.optString("repo"), item.optString("appName"),
                    item.optString("version"), item.optString("apkName"));
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized List<DownloadInfo> allDownloads() {
        List<DownloadInfo> out = new ArrayList<DownloadInfo>();
        try {
            JSONObject root = new JSONObject(prefs.getString(DOWNLOADS, "{}"));
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject item = root.optJSONObject(key);
                if (item == null) continue;
                long id;
                try { id = Long.parseLong(key); } catch (Exception e) { continue; }
                out.add(new DownloadInfo(id, item.optString("repo"), item.optString("appName"),
                        item.optString("version"), item.optString("apkName")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    public static synchronized void removeDownload(long id) {
        try {
            JSONObject root = new JSONObject(prefs.getString(DOWNLOADS, "{}"));
            root.remove(Long.toString(id));
            prefs.edit().putString(DOWNLOADS, root.toString()).apply();
        } catch (Exception ignored) { }
    }
}
