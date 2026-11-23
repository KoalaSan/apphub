package com.buddyapps.apphub;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Escanea las apps instaladas en el dispositivo y detecta actualizaciones de las que vinieron de AppHub. */
public class InstalledAppsActivity extends Activity {
    private LinearLayout box;
    private TextView summary;
    private EditText filter;
    private final List<Installed> all = new ArrayList<Installed>();
    private String query = "";

    private static final class Installed {
        String label, pkg, version;
        Drawable icon;
        StorePrefs.Tracked tracked;
        TextView state;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StorePrefs.init(this);
        GitHubApi.init(this);

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, "Apps instaladas", "Detectadas en tu dispositivo",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { finish(); }
                },
                new View.OnClickListener() {
                    @Override public void onClick(View v) { scan(); }
                }));
        root.addView(Ui.divider(this));

        ScrollView scroll = new ScrollView(this);
        box = Ui.vertical(this);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(box, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        LinearLayout top = Ui.card(this);
        summary = Ui.tv(this, "Buscando apps…", 14, Ui.TEXT, true);
        top.addView(summary);
        filter = new EditText(this);
        filter.setHint("Filtrar por nombre o paquete");
        filter.setSingleLine(true);
        filter.setTextColor(Ui.TEXT);
        filter.setHintTextColor(Ui.MUTED);
        android.graphics.drawable.GradientDrawable bg = Ui.rounded(Ui.BG, 10);
        bg.setStroke(Ui.dp(this, 1), Ui.LINE);
        filter.setBackground(bg);
        filter.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.topMargin = Ui.dp(this, 12);
        filter.setLayoutParams(fp);
        filter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                render();
            }
        });
        top.addView(filter);
        box.addView(top);
        Ui.margins(top, 0, 0, 0, 14, this);

        scan();
    }

    private void scan() {
        all.clear();
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo pi : packages) {
            if (pi == null || pi.applicationInfo == null) continue;
            ApplicationInfo ai = pi.applicationInfo;
            // Solo apps del usuario; oculta el sistema para que la lista sea útil.
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) continue;
            CharSequence label = pm.getApplicationLabel(ai);
            Installed item = new Installed();
            item.label = label == null ? pi.packageName : label.toString();
            item.pkg = pi.packageName;
            item.version = pi.versionName == null ? "" : pi.versionName;
            try { item.icon = pm.getApplicationIcon(ai); } catch (Exception e) { item.icon = null; }
            item.tracked = StorePrefs.trackedByPackage(item.pkg, item.label);
            all.add(item);
        }
        Collections.sort(all, new Comparator<Installed>() {
            @Override public int compare(Installed a, Installed b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        int fromHub = 0;
        for (Installed i : all) if (i.tracked != null) fromHub++;
        summary.setText(all.size() + " apps en tu dispositivo · " + fromHub + " desde AppHub");
        render();
        checkUpdates();
    }

    private void render() {
        // El primer hijo es la tarjeta de resumen; el resto son filas.
        while (box.getChildCount() > 1) box.removeViewAt(1);
        int shown = 0;
        for (final Installed item : all) {
            if (query.length() > 0
                    && !item.label.toLowerCase(Locale.ROOT).contains(query)
                    && !item.pkg.toLowerCase(Locale.ROOT).contains(query)) continue;
            shown++;
            LinearLayout row = Ui.card(this);
            LinearLayout info = Ui.vertical(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(ip);

            TextView title = Ui.tv(this, item.label, 14, Ui.TEXT, true);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            info.addView(title);
            info.addView(Ui.tv(this, item.pkg, 10, Ui.MUTED, false));

            if (item.tracked != null) {
                TextView badge = Ui.tv(this, "DE APPHUB", 10, Ui.GREEN, true);
                badge.setBackground(Ui.rounded(Ui.GREEN_BG, 8));
                badge.setPadding(Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2));
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                bp.topMargin = Ui.dp(this, 5);
                badge.setLayoutParams(bp);
                info.addView(badge);
            }

            item.state = Ui.tv(this,
                    item.version.length() > 0 ? "Versión " + item.version : "App del dispositivo",
                    11, Ui.MUTED, false);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.topMargin = Ui.dp(this, 5);
            item.state.setLayoutParams(sp);
            info.addView(item.state);
            row.addView(info);

            if (item.icon != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(item.icon);
                int s = Ui.dp(this, 42);
                iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ivp = (LinearLayout.LayoutParams) iv.getLayoutParams();
                ivp.gravity = Gravity.CENTER_VERTICAL;
                ivp.leftMargin = Ui.dp(this, 10);
                iv.setLayoutParams(ivp);
                row.addView(iv);
            }

            final Installed target = item;
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { open(target); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(this, 8);
            row.setLayoutParams(lp);
            box.addView(row);
            Anim.slideIn(row, Math.min(shown - 1, 8) * 35);
        }
        if (shown == 0) {
            TextView none = Ui.tv(this, "Ninguna app coincide con «" + query + "».", 13, Ui.MUTED, false);
            none.setPadding(Ui.dp(this, 8), Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 8));
            box.addView(none);
        }
    }

    private void checkUpdates() {
        for (final Installed item : all) {
            if (item.tracked == null || item.state == null) continue;
            final StorePrefs.Tracked t = item.tracked;
            item.state.setText("Comprobando actualizaciones…");
            GitHubApi.latestRelease(t.repo, false, new GitHubApi.Callback<ReleaseInfo>() {
                @Override public void onResult(ReleaseInfo release, Throwable error) {
                    if (isFinishing() || item.state == null) return;
                    if (error != null || release == null || !release.hasApk) {
                        item.state.setText("No se pudo comprobar");
                        return;
                    }
                    if (Versions.isNewer(release.tag, item.version)) {
                        item.state.setText("Actualización disponible: " + release.tag + " · toca para descargar");
                        item.state.setTextColor(Ui.GREEN);
                        final ReleaseInfo r = release;
                        item.state.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                AppInfo app = new AppInfo(t.repo, t.repo.split("/")[0],
                                        t.repo.split("/")[1], "", "https://github.com/" + t.repo, "", 0);
                                if (!InstallHelper.canInstall(InstalledAppsActivity.this)) {
                                    InstallHelper.openInstallSettings(InstalledAppsActivity.this);
                                    return;
                                }
                                long id = DownloadLauncher.enqueue(InstalledAppsActivity.this, app, r);
                                if (id < 0) Toast.makeText(InstalledAppsActivity.this,
                                        "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show();
                                else {
                                    Toast.makeText(InstalledAppsActivity.this,
                                            "Descargando " + release.tag, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(InstalledAppsActivity.this, DownloadsActivity.class));
                                }
                            }
                        });
                    } else {
                        item.state.setText("Al día · " + release.tag);
                        item.state.setTextColor(Ui.MUTED);
                        item.state.setOnClickListener(null);
                    }
                }
            });
        }
    }

    private void open(Installed item) {
        if (item.tracked != null && item.state != null && item.state.hasOnClickListeners()) {
            item.state.performClick();
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(item.pkg);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } else {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + item.pkg)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + item.pkg)));
            }
        }
    }
}
