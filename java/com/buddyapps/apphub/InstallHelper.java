package com.buddyapps.apphub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/** Abre el instalador del sistema con el APK descargado. */
public final class InstallHelper {
    private static final String CHANNEL = "apphub_install";
    private static final int NOTIFICATION_ID = 731;

    private InstallHelper() { }

    public static final String APK_MIME = "application/vnd.android.package-archive";

    public static boolean canInstall(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }

    public static void openInstallSettings(Context c) {
        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    public static Intent installIntent(Context c, Uri uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, APK_MIME);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    public static void notifyReady(Context c, Uri uri, String appName) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Instalación de apps",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("APK descargado, listo para instalar");
            nm.createNotificationChannel(ch);
        }
        Intent i = installIntent(c, uri);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                | Intent.FLAG_GRANT_READ_URI_PERMISSION;
        PendingIntent pi = PendingIntent.getActivity(c, NOTIFICATION_ID, i, flags);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Descarga lista: " + appName)
                .setContentText("Toca para instalar la app")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify(NOTIFICATION_ID, n);
    }
}
