package com.buddyapps.apphub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Detalle de una app de GitHub y descarga de su APK. */
public class AppDetailActivity extends Activity {
    private AppInfo app;
    private LinearLayout body;
    private LinearLayout readmeBox;
    private LinearLayout metaBox;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GitHubApi.init(this);
        StorePrefs.init(this);
        app = (AppInfo) getIntent().getSerializableExtra("app");
        if (app == null) {
            String fullName = getIntent().getStringExtra("fullName");
            if (fullName == null) { finish(); return; }
            app = new AppInfo(fullName, fullName.split("/")[0], fullName.split("/")[1],
                    "", "https://github.com/" + fullName, "", 0);
        }

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, app.displayName(), "Desde GitHub",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { finish(); }
                },
                new View.OnClickListener() {
                    @Override public void onClick(View v) { load(true); }
                }));
        root.addView(Ui.divider(this));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = Ui.vertical(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(body, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        load(false);
    }

    private void load(final boolean force) {
        body.removeAllViews();
        LinearLayout loading = Ui.card(this);
        loading.addView(Ui.tv(this, "Buscando el último release…", 14, Ui.MUTED, false));
        body.addView(loading);
        GitHubApi.latestRelease(app.fullName, force, new GitHubApi.Callback<ReleaseInfo>() {
            @Override public void onResult(ReleaseInfo release, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                body.removeAllViews();
                if (error != null) {
                    LinearLayout card = Ui.card(AppDetailActivity.this);
                    card.addView(Ui.tv(AppDetailActivity.this, "No se pudo cargar el release", 15, Ui.TEXT, true));
                    TextView msg = Ui.tv(AppDetailActivity.this, error.getMessage(), 13, Ui.MUTED, false);
                    Ui.margins(msg, 0, 6, 0, 0, AppDetailActivity.this);
                    card.addView(msg);
                    TextView retry = Ui.primaryButton(AppDetailActivity.this, "Reintentar");
                    Ui.margins(retry, 0, 12, 0, 0, AppDetailActivity.this);
                    retry.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { load(true); }
                    });
                    card.addView(retry);
                    body.addView(card);
                    return;
                }
                render(release);
            }
        });
    }

    private void render(final ReleaseInfo release) {
        LinearLayout hero = Ui.card(this);
        LinearLayout top = Ui.horizontal(this);
        ImageView avatar = new ImageView(this);
        int size = Ui.dp(this, 54);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Ui.dp(AppDetailActivity.this, 12));
            }
        });
        avatar.setClipToOutline(true);
        AvatarLoader.load(avatar, app.avatarUrl, app.displayName());
        top.addView(avatar);
        LinearLayout names = Ui.vertical(this);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        np.leftMargin = Ui.dp(this, 12);
        names.setLayoutParams(np);
        names.addView(Ui.tv(this, app.displayName(), 18, Ui.TEXT, true));
        names.addView(Ui.tv(this, app.fullName, 12, Ui.MUTED, false));
        top.addView(names);
        hero.addView(top);
        TextView desc = Ui.tv(this, app.description, 13, Ui.MUTED, false);
        Ui.margins(desc, 0, 12, 0, 0, this);
        hero.addView(desc);
        body.addView(hero);
        Ui.margins(hero, 0, 0, 0, 12, this);

        metaBox = Ui.vertical(this);
        body.addView(metaBox);

        readmeBox = Ui.vertical(this);
        body.addView(readmeBox);

        LinearLayout stats = Ui.card(this);
        LinearLayout statsRow = Ui.horizontal(this);
        statsRow.addView(stat("Estrellas", String.valueOf(app.stars)));
        statsRow.addView(stat("Versión", release.tag == null || release.tag.length() == 0 ? "—" : release.tag));
        statsRow.addView(stat("APK", release.hasApk ? readableSize(release.apkSize) : "no disponible"));
        stats.addView(statsRow);
        StorePrefs.Tracked tracked = StorePrefs.tracked(app.fullName);
        if (tracked != null) {
            TextView installed = Ui.tv(this, "Instalada desde AppHub: " + tracked.version,
                    12, Ui.GREEN, true);
            Ui.margins(installed, 0, 10, 0, 0, this);
            stats.addView(installed);
        }
        body.addView(stats);
        Ui.margins(stats, 0, 0, 0, 14, this);

        if (release.hasApk) {
            TextView download = Ui.primaryButton(this, "Descargar e instalar");
            download.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { startDownload(release); }
            });
            body.addView(download);
        } else {
            TextView noApk = Ui.tv(this, "Este release no incluye un APK. Mira los releases en GitHub.",
                    13, Ui.AMBER, false);
            noApk.setBackground(Ui.rounded(Ui.AMBER_BG, 12));
            noApk.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            body.addView(noApk);
        }

        TextView github = Ui.secondaryButton(this, "Abrir proyecto en GitHub");
        Ui.margins(github, 0, 10, 0, 0, this);
        final String url = app.htmlUrl == null || app.htmlUrl.length() == 0
                ? "https://github.com/" + app.fullName : app.htmlUrl;
        github.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
        body.addView(github);

        TextView note = Ui.tv(this,
                "El APK lo publica el autor del repositorio. AppHub solo lo descarga y te lleva al instalador del sistema.",
                11, Ui.MUTED, false);
        Ui.margins(note, 0, 16, 0, 0, this);
        body.addView(note);
        loadMeta();
        loadReadme();
    }

    private void loadMeta() {
        GitHubApi.repoMeta(app.fullName, false, new GitHubApi.Callback<RepoMeta>() {
            @Override public void onResult(RepoMeta meta, Throwable error) {
                if (isFinishing() || isDestroyed() || metaBox == null || meta == null) return;
                metaBox.removeAllViews();
                LinearLayout card = Ui.card(AppDetailActivity.this);
                card.addView(Ui.tv(AppDetailActivity.this, "El proyecto", 15, Ui.TEXT, true));

                LinearLayout chips = Ui.horizontal(AppDetailActivity.this);
                chips.setPadding(0, Ui.dp(AppDetailActivity.this, 10), 0, 0);
                if (meta.hasLicense()) {
                    TextView lic = Ui.tv(AppDetailActivity.this,
                            "Licencia " + meta.licenseLabel(), 11, Ui.GREEN, true);
                    lic.setBackground(Ui.rounded(Ui.GREEN_BG, 10));
                    lic.setPadding(Ui.dp(AppDetailActivity.this, 8), Ui.dp(AppDetailActivity.this, 4),
                            Ui.dp(AppDetailActivity.this, 8), Ui.dp(AppDetailActivity.this, 4));
                    chips.addView(lic);
                    if (meta.licenseUrl.length() > 0) {
                        final String url = meta.licenseUrl;
                        lic.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            }
                        });
                    }
                }
                if (meta.language.length() > 0) {
                    TextView lang = Ui.tv(AppDetailActivity.this, meta.language, 11, Ui.PRIMARY, true);
                    lang.setBackground(Ui.rounded(0xFFE8F0FF, 10));
                    lang.setPadding(Ui.dp(AppDetailActivity.this, 8), Ui.dp(AppDetailActivity.this, 4),
                            Ui.dp(AppDetailActivity.this, 8), Ui.dp(AppDetailActivity.this, 4));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.leftMargin = Ui.dp(AppDetailActivity.this, 6);
                    lang.setLayoutParams(lp);
                    chips.addView(lang);
                }
                card.addView(chips);

                TextView facts = Ui.tv(AppDetailActivity.this,
                        meta.forks + " forks  ·  " + meta.issues + " incidencias abiertas",
                        12, Ui.MUTED, false);
                Ui.margins(facts, 0, 10, 0, 0, AppDetailActivity.this);
                card.addView(facts);

                if (!meta.topics.isEmpty()) {
                    HorizontalScrollView ts = new HorizontalScrollView(AppDetailActivity.this);
                    ts.setHorizontalScrollBarEnabled(false);
                    LinearLayout strip = Ui.horizontal(AppDetailActivity.this);
                    for (String topic : meta.topics) {
                        TextView t = Ui.tv(AppDetailActivity.this, topic, 11, Ui.MUTED, false);
                        t.setBackground(Ui.rounded(Ui.BG, 8));
                        t.setPadding(Ui.dp(AppDetailActivity.this, 8), Ui.dp(AppDetailActivity.this, 4),
                                Ui.dp(AppDetailActivity.this, 8), Ui.dp(AppDetailActivity.this, 4));
                        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        tp.rightMargin = Ui.dp(AppDetailActivity.this, 6);
                        t.setLayoutParams(tp);
                        strip.addView(t);
                    }
                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    sp.topMargin = Ui.dp(AppDetailActivity.this, 10);
                    strip.setLayoutParams(sp);
                    ts.addView(strip);
                    card.addView(ts);
                }

                if (meta.homepage.length() > 0) {
                    final String url = meta.homepage.startsWith("http") ? meta.homepage
                            : "https://" + meta.homepage;
                    TextView home = Ui.tv(AppDetailActivity.this, "Sitio web del proyecto →",
                            12, Ui.PRIMARY, true);
                    Ui.margins(home, 0, 12, 0, 0, AppDetailActivity.this);
                    home.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        }
                    });
                    card.addView(home);
                }
                metaBox.addView(card);
                Ui.margins(card, 0, 0, 0, 12, AppDetailActivity.this);
                Anim.fadeIn(card);
            }
        });
    }

    private void loadReadme() {
        GitHubApi.readme(app.fullName, false, new GitHubApi.Callback<ReadmeInfo>() {
            @Override public void onResult(ReadmeInfo info, Throwable error) {
                if (isFinishing() || isDestroyed() || readmeBox == null) return;
                readmeBox.removeAllViews();
                if (info == null) return;
                if (info.intro != null && info.intro.length() > 0) {
                    LinearLayout card = Ui.card(AppDetailActivity.this);
                    card.addView(Ui.tv(AppDetailActivity.this, "Descripción", 15, Ui.TEXT, true));
                    TextView text = Ui.tv(AppDetailActivity.this, info.intro, 13, Ui.MUTED, false);
                    Ui.margins(text, 0, 8, 0, 0, AppDetailActivity.this);
                    card.addView(text);
                    readmeBox.addView(card);
                    Ui.margins(card, 0, 0, 0, 12, AppDetailActivity.this);
                }
                if (info.images != null && !info.images.isEmpty()) {
                    LinearLayout card = Ui.card(AppDetailActivity.this);
                    card.addView(Ui.tv(AppDetailActivity.this,
                            "Capturas (" + info.images.size() + ")", 15, Ui.TEXT, true));
                    HorizontalScrollView scroller = new HorizontalScrollView(AppDetailActivity.this);
                    scroller.setHorizontalScrollBarEnabled(false);
                    LinearLayout strip = Ui.horizontal(AppDetailActivity.this);
                    int w = Ui.dp(AppDetailActivity.this, 180);
                    int h = Ui.dp(AppDetailActivity.this, 120);
                    for (final String url : info.images) {
                        ImageView image = new ImageView(AppDetailActivity.this);
                        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(w, h);
                        ip.rightMargin = Ui.dp(AppDetailActivity.this, 8);
                        image.setLayoutParams(ip);
                        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        image.setOutlineProvider(new ViewOutlineProvider() {
                            @Override public void getOutline(View view, Outline outline) {
                                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                                        Ui.dp(AppDetailActivity.this, 10));
                            }
                        });
                        image.setClipToOutline(true);
                        RemoteImageLoader.load(image, url, w, h);
                        image.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                // El visor vive dentro de la app: nunca se abre el navegador.
                                Intent viewer = new Intent(AppDetailActivity.this, ImageViewerActivity.class);
                                viewer.putExtra("url", url);
                                startActivity(viewer);
                                overridePendingTransition(android.R.anim.fade_in,
                                        android.R.anim.fade_out);
                            }
                        });
                        strip.addView(image);
                    }
                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    sp.topMargin = Ui.dp(AppDetailActivity.this, 10);
                    strip.setLayoutParams(sp);
                    scroller.addView(strip);
                    card.addView(scroller);
                    readmeBox.addView(card);
                }
            }
        });
    }

    private LinearLayout stat(String label, String value) {
        LinearLayout s = Ui.vertical(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        s.setLayoutParams(lp);
        s.addView(Ui.tv(this, value, 15, Ui.TEXT, true));
        s.addView(Ui.tv(this, label, 11, Ui.MUTED, false));
        return s;
    }

    private void startDownload(final ReleaseInfo release) {
        if (!InstallHelper.canInstall(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permitir instalar apps")
                    .setMessage("Activa «Instalar apps desconocidas» para AppHub en Ajustes y vuelve aquí.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Ir a Ajustes", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int which) {
                            InstallHelper.openInstallSettings(AppDetailActivity.this);
                        }
                    }).show();
            return;
        }
        long id = DownloadLauncher.enqueue(this, app, release);
        if (id == -1) Toast.makeText(this, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(this, "Descarga iniciada · verás el progreso y podrás instalar al terminar",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DownloadsActivity.class));
        }
    }

    private static String readableSize(long bytes) {
        if (bytes <= 0) return "—";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024f) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }
}
