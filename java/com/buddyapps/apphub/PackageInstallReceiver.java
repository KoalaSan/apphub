package com.buddyapps.apphub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/** Confirma qué APKs descargados desde AppHub acaban de instalarse realmente. */
public final class PackageInstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) return;
        String action = intent.getAction();
        String pkg = intent.getData().getSchemeSpecificPart();
        if (pkg == null || pkg.length() == 0) return;
        StorePrefs.init(context);

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            // Al actualizar, Android envía REMOVED + REPLACED: solo borramos si no hay reemplazo.
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (!replacing) {
                for (StorePrefs.Tracked t : StorePrefs.allTracked()) {
                    if (pkg.equals(t.packageName)) StorePrefs.removeTracked(t.repo);
                }
            }
            return;
        }

        if (!Intent.ACTION_PACKAGE_ADDED.equals(action)
                && !Intent.ACTION_PACKAGE_REPLACED.equals(action)) return;

        String installedVersion = installedVersion(context, pkg);
        StorePrefs.PendingInstall pending = StorePrefs.pendingInstall(pkg);
        if (pending != null) {
            StorePrefs.track(pending.repo,
                    installedVersion.length() > 0 ? installedVersion : pending.version,
                    pending.apkName, pkg, pending.appName);
            StorePrefs.removePendingInstall(pkg);
            return;
        }
        // Ya estaba registrada (p. ej. una actualización instalada a mano): refresca la versión.
        for (StorePrefs.Tracked t : StorePrefs.allTracked()) {
            if (pkg.equals(t.packageName)) {
                StorePrefs.track(t.repo, installedVersion, t.apkName, t.packageName, t.appName);
                break;
            }
        }
    }

    private static String installedVersion(Context c, String pkg) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(pkg, 0);
            return pi.versionName == null ? "" : pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
