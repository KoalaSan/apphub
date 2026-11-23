package com.buddyapps.apphub;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Publica una app del usuario en el catálogo público de AppHub. */
public class PublishActivity extends Activity {
    private LinearLayout box;
    private List<AppInfo> repos = new ArrayList<AppInfo>();
    private List<CatalogItem> catalog = new ArrayList<CatalogItem>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GitHubApi.init(this);
        StorePrefs.init(this);

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, "Publicar una app", "Aparecerá para todos los usuarios",
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

        if (!StorePrefs.hasToken()) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.tv(this, "Conecta tu GitHub", 16, Ui.TEXT, true));
            TextView info = Ui.tv(this,
                    "Para publicar apps necesitas un token de GitHub con permisos de escritura en "
                            + AppConfig.CATALOG_REPO + ".",
                    13, Ui.MUTED, false);
            Ui.margins(info, 0, 6, 0, 0, this);
            card.addView(info);
            TextView connect = Ui.primaryButton(this, "Conectar GitHub");
            Ui.margins(connect, 0, 14, 0, 0, this);
            connect.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new android.content.Intent(PublishActivity.this, TokenActivity.class));
                }
            });
            card.addView(connect);
            box.addView(card);
            return;
        }
        load();
    }

    private void load() {
        box.removeAllViews();
        LinearLoading.show(box, this, "Cargando tus apps y el catálogo…");
        GitHubApi.catalog(false, new GitHubApi.Callback<List<CatalogItem>>() {
            @Override public void onResult(List<CatalogItem> items, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                catalog = items == null ? new ArrayList<CatalogItem>() : items;
                GitHubApi.userRepos(false, new GitHubApi.Callback<List<AppInfo>>() {
                    @Override public void onResult(List<AppInfo> list, Throwable error2) {
                        if (isFinishing() || isDestroyed()) return;
                        repos = list == null ? new ArrayList<AppInfo>() : list;
                        renderList(error2);
                    }
                });
            }
        });
    }

    private void renderList(Throwable error) {
        box.removeAllViews();
        LinearLayout intro = Ui.card(this);
        intro.addView(Ui.tv(this, "Elige una app para publicar", 16, Ui.TEXT, true));
        TextView info = Ui.tv(this,
                "Todas las personas que usen AppHub podrán verla y descargar su APK desde los Releases de GitHub.",
                13, Ui.MUTED, false);
        Ui.margins(info, 0, 6, 0, 0, this);
        intro.addView(info);
        box.addView(intro);
        Ui.margins(intro, 0, 0, 0, 16, this);

        if (error != null) {
            TextView retry = Ui.secondaryButton(this, "Reintentar");
            retry.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { load(); }
            });
            box.addView(retry);
            return;
        }
        if (repos.isEmpty()) {
            box.addView(Ui.tv(this, "Tu cuenta no tiene repositorios visibles.", 13, Ui.MUTED, false));
            return;
        }
        for (final AppInfo app : repos) {
            boolean published = isPublished(app.fullName);
            LinearLayout row = Ui.card(this);
            row.addView(Ui.tv(this, app.displayName(), 15, Ui.TEXT, true));
            TextView d = Ui.tv(this, app.description, 12, Ui.MUTED, false);
            Ui.margins(d, 0, 3, 0, 0, this);
            d.setMaxLines(2);
            row.addView(d);
            TextView state = Ui.tv(this,
                    published ? "PUBLICADA · toca para editar" : "Publicar en el catálogo",
                    12, published ? Ui.GREEN : Ui.PRIMARY, true);
            Ui.margins(state, 0, 10, 0, 0, this);
            row.addView(state);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = Ui.dp(this, 8);
            row.setLayoutParams(rp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showForm(app); }
            });
            box.addView(row);
        }
    }

    private boolean isPublished(String repo) {
        for (CatalogItem it : catalog) if (repo.equals(it.repo)) return true;
        return false;
    }

    private void showForm(final AppInfo app) {
        box.removeAllViews();
        TextView back = Ui.tv(this, "← Mis repositorios", 12, Ui.PRIMARY, true);
        Ui.margins(back, 0, 0, 0, 12, this);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { renderList(null); }
        });
        box.addView(back);

        LinearLayout card = Ui.card(this);
        card.addView(Ui.tv(this, "Datos de la app", 16, Ui.TEXT, true));

        final EditText name = edit(app.displayName(), "Nombre de la app");
        card.addView(name);
        final EditText desc = edit(app.description == null ? "" : app.description, "Descripción");
        card.addView(desc);

        card.addView(Ui.tv(this, "Categoría", 12, Ui.MUTED, false));
        final String[] chosen = { app.category == null || Categories.ALL.equals(app.category)
                ? "Herramientas" : app.category };
        HorizontalScrollView catScroll = new HorizontalScrollView(this);
        catScroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout chips = Ui.horizontal(this);
        final TextView[] chipViews = new TextView[Categories.NAMES.length - 1];
        for (int i = 1; i < Categories.NAMES.length; i++) {
            final String cat = Categories.NAMES[i];
            final int idx = i - 1;
            TextView chip = Ui.tv(this, cat, 12, Ui.MUTED, true);
            chip.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.rightMargin = Ui.dp(this, 6);
            chip.setLayoutParams(cp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    chosen[0] = cat;
                    for (int j = 0; j < chipViews.length; j++) {
                        chipViews[j].setBackground(Ui.rounded(Ui.CARD, 16));
                        chipViews[j].setTextColor(Ui.MUTED);
                    }
                    chip.setBackground(Ui.rounded(Ui.PRIMARY, 16));
                    chip.setTextColor(0xFFFFFFFF);
                }
            });
            chips.addView(chip);
            chipViews[idx] = chip;
        }
        catScroll.addView(chips);
        card.addView(catScroll);
        for (int j = 0; j < chipViews.length; j++) {
            if (Categories.NAMES[j + 1].equals(chosen[0])) {
                chipViews[j].setBackground(Ui.rounded(Ui.PRIMARY, 16));
                chipViews[j].setTextColor(0xFFFFFFFF);
            }
        }

        TextView publish = Ui.primaryButton(this,
                isPublished(app.fullName) ? "Actualizar en el catálogo" : "Publicar en el catálogo");
        Ui.margins(publish, 0, 16, 0, 0, this);
        publish.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                publish(app, name.getText().toString().trim(), desc.getText().toString().trim(), chosen[0]);
            }
        });
        card.addView(publish);

        if (isPublished(app.fullName)) {
            TextView remove = Ui.tv(this, "Quitar del catálogo", 13, Ui.RED, false);
            Ui.margins(remove, 0, 12, 0, 0, this);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { unpublish(app); }
            });
            card.addView(remove);
        }
        box.addView(card);

        TextView hint = Ui.tv(this,
                "La versión y el APK se toman del último Release de GitHub automáticamente.",
                11, Ui.MUTED, false);
        Ui.margins(hint, 0, 10, 0, 0, this);
        box.addView(hint);

        GitHubApi.latestRelease(app.fullName, false, new GitHubApi.Callback<ReleaseInfo>() {
            @Override public void onResult(ReleaseInfo release, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                if (release != null && release.hasApk && release.tag.length() > 0) {
                    hint.setText("Release detectado: " + release.tag + " · " + release.apkName);
                    hint.setTextColor(Ui.GREEN);
                } else {
                    hint.setText("Este repo aún no tiene un Release con APK. Publica uno en GitHub para que se pueda descargar.");
                    hint.setTextColor(Ui.AMBER);
                }
            }
        });
    }

    private EditText edit(String value, String hint) {
        EditText f = new EditText(this);
        f.setText(value == null ? "" : value);
        f.setHint(hint);
        f.setTextColor(Ui.TEXT);
        f.setHintTextColor(Ui.MUTED);
        GradientDrawable bg = Ui.rounded(Ui.BG, 10);
        bg.setStroke(Ui.dp(this, 1), Ui.LINE);
        f.setBackground(bg);
        f.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.topMargin = Ui.dp(this, 10);
        f.setLayoutParams(fp);
        return f;
    }

    private void publish(final AppInfo app, final String name, final String desc, final String category) {
        if (name.length() == 0) {
            Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Publicando en GitHub…", Toast.LENGTH_SHORT).show();
        GitHubApi.latestRelease(app.fullName, true, new GitHubApi.Callback<ReleaseInfo>() {
            @Override public void onResult(ReleaseInfo release, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                List<CatalogItem> next = new ArrayList<CatalogItem>();
                for (CatalogItem it : catalog) if (!app.fullName.equals(it.repo)) next.add(it);
                String tag = release == null ? "" : release.tag;
                String apkName = release == null ? "" : release.apkName;
                String apkUrl = release == null ? "" : release.apkUrl;
                String releaseUrl = "https://github.com/" + app.fullName + "/releases";
                String avatar = app.avatarUrl;
                next.add(0, new CatalogItem(name, app.owner, app.fullName, desc, category,
                        tag, apkName, apkUrl, releaseUrl));
                GitHubApi.publishCatalog(next, new GitHubApi.Callback<Void>() {
                    @Override public void onResult(Void ignored, Throwable err) {
                        if (isFinishing() || isDestroyed()) return;
                        if (err != null) {
                            Toast.makeText(PublishActivity.this, err.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(PublishActivity.this, "App publicada en el catálogo", Toast.LENGTH_SHORT).show();
                        load();
                    }
                });
            }
        });
    }

    private void unpublish(final AppInfo app) {
        List<CatalogItem> next = new ArrayList<CatalogItem>();
        for (CatalogItem it : catalog) if (!app.fullName.equals(it.repo)) next.add(it);
        GitHubApi.publishCatalog(next, new GitHubApi.Callback<Void>() {
            @Override public void onResult(Void ignored, Throwable err) {
                if (isFinishing() || isDestroyed()) return;
                if (err != null) Toast.makeText(PublishActivity.this, err.getMessage(), Toast.LENGTH_LONG).show();
                else {
                    Toast.makeText(PublishActivity.this, "App quitada del catálogo", Toast.LENGTH_SHORT).show();
                    load();
                }
            }
        });
    }
}
