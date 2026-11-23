package com.buddyapps.apphub;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Cuando termina una descarga guarda el paquete del APK y ofrece instalarlo. */
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
                    // Se registra como "pendiente de instalar"; PackageInstallReceiver lo
                    // confirmará cuando el sistema notifique que la app ya está instalada.
                    StorePrefs.addPendingInstall(packageName, info.repo, info.version,
                            info.apkName, info.appName);
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

    /**
     * Lee el nombre de paquete del APK. En Android 10+ el URI es `content://` y no da una ruta
     * directa, así que se copia a la caché de la app y se lee desde ahí (siempre funciona).
     */
    private static String packageOf(Context c, Uri uri) {
        if (uri == null) return "";
        InputStream in = null;
        OutputStream out = null;
        File temp = null;
        try {
            in = c.getContentResolver().openInputStream(uri);
            if (in == null) return "";
            temp = File.createTempFile("apphub_apk", ".apk", c.getCacheDir());
            out = new FileOutputStream(temp);
            byte[] buf = new byte[16384];
            int n;
            long total = 0;
            while ((n = in.read(buf)) != -1 && total < 80L * 1024 * 1024) {
                out.write(buf, 0, n);
                total += n;
            }
            out.close();
            out = null;
            android.content.pm.PackageInfo pi = c.getPackageManager()
                    .getPackageArchiveInfo(temp.getAbsolutePath(), 0);
            return (pi == null || pi.packageName == null) ? "" : pi.packageName;
        } catch (Exception e) {
            return "";
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) { }
            if (in != null) try { in.close(); } catch (Exception ignored) { }
            if (temp != null) try { //noinspection ResultOfMethodCallIgnored
                temp.delete();
            } catch (Exception ignored) { }
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
