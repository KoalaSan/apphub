package com.buddyapps.apphub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Lista de descargas con barra de progreso en vivo y botón para instalar al terminar. */
public class DownloadsActivity extends Activity {
    private LinearLayout box;
    private TextView empty;
    private final Handler handler = new Handler();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GitHubApi.init(this);
        StorePrefs.init(this);

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, "Descargas", "APKs de GitHub",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { finish(); }
                }, null));
        root.addView(Ui.divider(this));
        ScrollView scroll = new ScrollView(this);
        box = Ui.vertical(this);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(box, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        empty = Ui.tv(this, "No hay descargas todavía. Descarga una app y aparecerá aquí.",
                13, Ui.MUTED, false);
        empty.setPadding(Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 8));
        box.addView(empty);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void refresh() {
        List<StorePrefs.DownloadInfo> downloads = StorePrefs.allDownloads();
        empty.setVisibility(downloads.isEmpty() ? View.VISIBLE : View.GONE);
        box.removeAllViews();
        if (downloads.isEmpty()) {
            box.addView(empty);
            return;
        }
        boolean active = false;
        for (StorePrefs.DownloadInfo info : downloads) {
            addRow(info);
            if (state(info.id).active) active = true;
        }
        TextView clear = Ui.tv(this, "Borrar historial", 12, Ui.MUTED, false);
        Ui.margins(clear, 0, 6, 0, 0, this);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                for (StorePrefs.DownloadInfo i : StorePrefs.allDownloads()) StorePrefs.removeDownload(i.id);
                refresh();
            }
        });
        box.addView(clear);
        if (active) handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isFinishing() && !isDestroyed()) refresh();
            }
        }, 700);
    }

    private void addRow(final StorePrefs.DownloadInfo info) {
        DownloadState st = state(info.id);
        LinearLayout card = Ui.card(this);
        card.addView(Ui.tv(this, info.appName.length() > 0 ? info.appName : "APK", 15, Ui.TEXT, true));
        TextView sub = Ui.tv(this,
                info.version == null || info.version.length() == 0 ? "Descargando desde GitHub" : "Versión " + info.version,
                12, Ui.MUTED, false);
        Ui.margins(sub, 0, 3, 0, 0, this);
        card.addView(sub);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 8));
        bp.topMargin = Ui.dp(this, 12);
        bar.setLayoutParams(bp);
        if (st.active) {
            bar.setIndeterminate(st.percent <= 0);
            if (st.percent > 0) Anim.progress(bar, st.percent);
        } else {
            Anim.progress(bar, st.success ? 100 : 0);
            bar.setIndeterminate(false);
        }
        card.addView(bar);

        TextView status = Ui.tv(this, st.label, 12,
                st.success ? Ui.GREEN : (st.failed ? Ui.RED : Ui.MUTED), false);
        Ui.margins(status, 0, 8, 0, 0, this);
        card.addView(status);

        if (st.success) {
            TextView install = Ui.primaryButton(this, "Instalar ahora");
            Ui.margins(install, 0, 12, 0, 0, this);
            install.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { promptInstall(info); }
            });
            card.addView(install);
        } else if (st.failed) {
            TextView remove = Ui.tv(this, "Quitar de la lista", 12, Ui.MUTED, false);
            Ui.margins(remove, 0, 10, 0, 0, this);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    StorePrefs.removeDownload(info.id);
                    refresh();
                }
            });
            card.addView(remove);
        }

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = Ui.dp(this, 8);
        card.setLayoutParams(cp);
        box.addView(card);
    }

    private void promptInstall(final StorePrefs.DownloadInfo info) {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        final Uri uri = dm.getUriForDownloadedFile(info.id);
        if (uri == null) {
            Toast.makeText(this, "El archivo ya no está disponible, vuelve a descargarlo", Toast.LENGTH_LONG).show();
            return;
        }
        if (!InstallHelper.canInstall(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permitir instalar apps")
                    .setMessage("Activa «Instalar apps desconocidas» para AppHub en Ajustes y vuelve aquí.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Ir a Ajustes", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int which) {
                            InstallHelper.openInstallSettings(DownloadsActivity.this);
                        }
                    }).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("¿Instalar " + info.appName + "?")
                .setMessage("El APK está listo. El instalador del sistema se abrirá para que lo revises.")
                .setNegativeButton("Ahora no", null)
                .setPositiveButton("Instalar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        InstallHelper.install(DownloadsActivity.this, uri);
                    }
                }).show();
    }

    private static final class DownloadState {
        boolean active, success, failed;
        int percent;
        String label = "Preparando…";
    }

    private DownloadState state(long id) {
        DownloadState s = new DownloadState();
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return s;
        Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
        try {
            if (c == null || !c.moveToFirst()) {
                s.failed = true;
                s.label = "No se encontró la descarga";
                return s;
            }
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            long done = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            if (total > 0) s.percent = (int) (done * 100 / total);
            switch (status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    s.success = true;
                    s.label = "Completada · toca «Instalar ahora»";
                    break;
                case DownloadManager.STATUS_FAILED:
                    s.failed = true;
                    int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    s.label = "Falló (" + reasonText(reason) + ")";
                    break;
                case DownloadManager.STATUS_RUNNING:
                    s.active = true;
                    s.label = "Descargando… " + s.percent + "%";
                    break;
                case DownloadManager.STATUS_PENDING:
                    s.active = true;
                    s.label = "En cola…";
                    break;
                case DownloadManager.STATUS_PAUSED:
                    s.active = true;
                    s.label = "En espera · " + s.percent + "%";
                    break;
                default:
                    s.label = "Preparando…";
                    break;
            }
        } finally {
            if (c != null) c.close();
        }
        return s;
    }

    private static String reasonText(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME: return "no se puede reanudar";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND: return "sin almacenamiento";
            case DownloadManager.ERROR_FILE_ERROR: return "error de archivo";
            case DownloadManager.ERROR_HTTP_DATA_ERROR: return "error de red";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE: return "sin espacio";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS: return "demasiadas redirecciones";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: return "respuesta del servidor";
            default: return "desconocido";
        }
    }
}
