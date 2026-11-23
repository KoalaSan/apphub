package com.buddyapps.apphub;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/** Cuando termina una descarga, registra la versión y avisa de que ya se puede instalar. */
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
            if (status != DownloadManager.STATUS_SUCCESSFUL) return;
            Uri uri = dm.getUriForDownloadedFile(id);
            StorePrefs.DownloadInfo info = StorePrefs.takeDownload(id);
            if (uri == null || info == null) return;
            if (info.repo != null && info.repo.length() > 0) {
                StorePrefs.track(info.repo, info.version, info.apkName);
            }
            InstallHelper.notifyReady(context, uri, info.appName);
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
