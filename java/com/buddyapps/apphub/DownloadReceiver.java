package com.buddyapps.apphub;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/** Cuando termina una descarga, registra la versión y pregunta si se quiere instalar. */
public final class DownloadReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if (id == -1) return;
        StorePrefs.init(context);
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        DownloadManager.Query q = new DownloadManager.Query().setFilterById(id);
        Cursor cursor = dm.query(q);
        try {
            if (cursor == null || !cursor.moveToFirst()) return;
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            Uri uri = dm.getUriForDownloadedFile(id);
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                StorePrefs.DownloadInfo info = StorePrefs.getDownload(id);
                if (info != null && info.repo != null && info.repo.length() > 0) {
                    String packageName = packageOf(context, uri);
                    StorePrefs.track(info.repo, info.version, info.apkName,
                            packageName, info.appName);
                }
                String appName = info == null ? null : info.appName;
                InstallHelper.notifyReady(context, uri, appName == null ? "APK" : appName);
                launchPrompt(context, id, appName, uri);
            } else if (status == DownloadManager.STATUS_FAILED) {
                InstallHelper.notifyFailed(context);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Lee el nombre de paquete del APK descargado para poder reconocerlo entre las apps instaladas. */
    private static String packageOf(Context c, Uri uri) {
        if (uri == null) return "";
        String path = uri.getPath();
        if ("content".equals(uri.getScheme())) {
            Cursor cc = null;
            try {
                cc = c.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cc != null && cc.moveToFirst()) {
                    int i = cc.getColumnIndex("_data");
                    if (i >= 0 && cc.getString(i) != null) path = cc.getString(i);
                }
            } catch (Exception ignored) {
            } finally {
                if (cc != null) cc.close();
            }
        }
        if (path == null || path.length() == 0) return "";
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageArchiveInfo(path, 0);
            return (pi == null || pi.packageName == null) ? "" : pi.packageName;
        } catch (Exception e) {
            return "";
        }
    }

    private static void launchPrompt(Context c, long id, String appName, Uri uri) {
        try {
            Intent i = new Intent(c, InstallPromptActivity.class);
            i.putExtra("id", id);
            i.putExtra("appName", appName);
            i.putExtra("uri", uri == null ? null : uri.toString());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception ignored) {
            // En Android 10+ puede estar restringido abrir Activity desde segundo plano;
            // la notificación sigue ofreciendo la instalación.
        }
    }
}
