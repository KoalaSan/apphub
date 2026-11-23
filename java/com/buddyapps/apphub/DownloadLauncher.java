package com.buddyapps.apphub;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

/** Encola el APK del release en DownloadManager y recuerda a qué app pertenece. */
public final class DownloadLauncher {
    private DownloadLauncher() { }

    public static long enqueue(Context c, AppInfo app, ReleaseInfo release) {
        if (release == null || !release.hasApk || release.apkUrl == null) return -1;
        try {
            DownloadManager dm = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) return -1;
            String name = safeName(release.apkName);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(release.apkUrl));
            req.setTitle(app.displayName() + " " + release.tag);
            req.setDescription("Descargado desde GitHub");
            req.setMimeType(InstallHelper.APK_MIME);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AppHub/" + name);
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            long id = dm.enqueue(req);
            StorePrefs.addDownload(id, app.fullName, app.displayName(), release.tag, release.apkName);
            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String safeName(String name) {
        if (name == null || name.length() == 0) return "app.apk";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
