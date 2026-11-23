package com.buddyapps.apphub;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pantalla principal: Explorar, Buscar, Mis apps y Actualizaciones. */
public class MainActivity extends Activity {
    private static final int TAB_EXPLORE = 0, TAB_SEARCH = 1, TAB_CREATOR = 2,
            TAB_MINE = 3, TAB_UPDATES = 4;
    private static final String[] NAV_LABELS = {"Explorar", "Buscar", "Creador", "Mis apps", "Actualizaciones"};
    private static final int[] NAV_ICONS = {R.drawable.ic_explore, R.drawable.ic_search,
            R.drawable.ic_creator, R.drawable.ic_apps, R.drawable.ic_updates};

    private final Page[] pages = new Page[5];
    private final LinearLayout[] navItems = new LinearLayout[5];
    private final TextView[] navLabels = new TextView[5];
    private final ImageView[] navIcons = new ImageView[5];
    private final Map<String, TextView> versionLabels = new HashMap<String, TextView>();
    private final Map<String, TextView> photoLabels = new HashMap<String, TextView>();
    private final Map<String, TextView> updateLabels = new HashMap<String, TextView>();

    private LinearLayout exploreList, searchList, mineList, updatesAppList, mineCard, selfCard;
    private LinearLayout categoryStrip, creatorList;
    private Status exploreStatus, searchStatus, mineStatus, updatesStatus, creatorStatus;
    private TextView selfResult, selfDownload, searchHeading;
    private EditText searchField, developerField;
    private final List<AppInfo> allExploreApps = new ArrayList<AppInfo>();
    private String selectedCategory = Categories.ALL;
    private int explorePage = 1;
    private int tab = TAB_EXPLORE;
    private boolean exploreLoaded;

    private static final class Page {
        ScrollView scroll;
        LinearLayout box;
    }

    private static final class Status {
        LinearLayout view;
        TextView text;
        TextView retry;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GitHubApi.init(this);
        StorePrefs.init(this);

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, "AppHub", "Apps y actualizaciones desde GitHub", null,
                new View.OnClickListener() {
                    @Override public void onClick(View v) { refreshTab(); }
                }));
        root.addView(Ui.divider(this));

        FrameLayout content = new FrameLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        pages[TAB_EXPLORE] = buildExplorePage();
        pages[TAB_SEARCH] = buildSearchPage();
        pages[TAB_CREATOR] = buildCreatorPage();
        pages[TAB_MINE] = buildMinePage();
        pages[TAB_UPDATES] = buildUpdatesPage();
        for (Page p : pages) content.addView(p.scroll);
        root.addView(content);

        root.addView(Ui.divider(this));
        root.addView(buildNav());
        setContentView(root);

        selectTab(TAB_EXPLORE);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (tab == TAB_MINE) loadMine();
        if (tab == TAB_UPDATES) loadUpdates();
    }

    private Page newPage() {
        Page p = new Page();
        p.scroll = new ScrollView(this);
        p.scroll.setFillViewport(true);
        p.box = Ui.vertical(this);
        p.box.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 28));
        p.scroll.addView(p.box, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return p;
    }

    private Page buildExplorePage() {
        Page p = newPage();
        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.tv(this, "Todo GitHub, en forma de tienda", 18, Ui.TEXT, true));
        TextView body = Ui.tv(this, "Descubre proyectos que publican sus APKs en GitHub y tenlos siempre actualizados.",
                13, Ui.MUTED, false);
        Ui.margins(body, 0, 6, 0, 0, this);
        hero.addView(body);
        p.box.addView(hero);
        Ui.margins(hero, 0, 0, 0, 14, this);

        p.box.addView(Ui.tv(this, "Categorías", 13, Ui.MUTED, false));
        HorizontalScrollView catScroll = new HorizontalScrollView(this);
        catScroll.setHorizontalScrollBarEnabled(false);
        categoryStrip = Ui.horizontal(this);
        catScroll.addView(categoryStrip, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        p.box.addView(catScroll);
        Ui.margins(catScroll, 0, 6, 0, 14, this);

        LinearLayout creatorBanner = Ui.card(this);
        creatorBanner.addView(Ui.tv(this, "Apps del creador · @" + AppConfig.CREATOR_LOGIN, 14, Ui.TEXT, true));
        TextView creatorBody = Ui.tv(this, "Las apps que publica " + AppConfig.CREATOR_LOGIN + " en GitHub, en un solo lugar.",
                12, Ui.MUTED, false);
        Ui.margins(creatorBody, 0, 4, 0, 0, this);
        creatorBanner.addView(creatorBody);
        TextView seeCreator = Ui.tv(this, "Ver apps del creador →", 12, Ui.PRIMARY, true);
        Ui.margins(seeCreator, 0, 8, 0, 0, this);
        seeCreator.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { selectTab(TAB_CREATOR); }
        });
        creatorBanner.addView(seeCreator);
        p.box.addView(creatorBanner);
        Ui.margins(creatorBanner, 0, 0, 0, 16, this);

        p.box.addView(sectionTitle("Apps destacadas"));
        exploreList = Ui.vertical(this);
        p.box.addView(exploreList, rowParams());
        exploreStatus = makeStatus();
        p.box.addView(exploreStatus.view);
        TextView note = Ui.tv(this, "Los APKs los publican sus propios autores. Revisa la fuente antes de instalar.",
                11, Ui.MUTED, false);
        Ui.margins(note, 0, 12, 0, 0, this);
        p.box.addView(note);
        return p;
    }

    private Page buildSearchPage() {
        Page p = newPage();
        LinearLayout card = Ui.card(this);
        card.addView(Ui.tv(this, "Buscar en GitHub", 16, Ui.TEXT, true));
        searchField = new EditText(this);
        searchField.setHint("Nombre, tema o palabra clave");
        searchField.setSingleLine(true);
        searchField.setTextColor(Ui.TEXT);
        searchField.setHintTextColor(Ui.MUTED);
        GradientDrawable field = Ui.rounded(Ui.BG, 10);
        field.setStroke(Ui.dp(this, 1), Ui.LINE);
        searchField.setBackground(field);
        searchField.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.topMargin = Ui.dp(this, 12);
        searchField.setLayoutParams(fp);
        card.addView(searchField);
        TextView go = Ui.primaryButton(this, "Buscar apps");
        Ui.margins(go, 0, 10, 0, 0, this);
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { loadSearch(true); }
        });
        card.addView(go);
        p.box.addView(card);

        TextView tip = Ui.tv(this, "Prueba con: calculator, markdown, video player, topic:android-app",
                11, Ui.MUTED, false);
        Ui.margins(tip, 4, 8, 4, 14, this);
        p.box.addView(tip);

        LinearLayout devCard = Ui.card(this);
        devCard.addView(Ui.tv(this, "Buscar por desarrollador", 16, Ui.TEXT, true));
        developerField = new EditText(this);
        developerField.setHint("Usuario de GitHub, sin @");
        developerField.setSingleLine(true);
        developerField.setTextColor(Ui.TEXT);
        developerField.setHintTextColor(Ui.MUTED);
        GradientDrawable devBg = Ui.rounded(Ui.BG, 10);
        devBg.setStroke(Ui.dp(this, 1), Ui.LINE);
        developerField.setBackground(devBg);
        developerField.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout.LayoutParams dfp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dfp.topMargin = Ui.dp(this, 12);
        developerField.setLayoutParams(dfp);
        devCard.addView(developerField);
        TextView devGo = Ui.primaryButton(this, "Ver apps de este desarrollador");
        Ui.margins(devGo, 0, 10, 0, 0, this);
        devGo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { loadDeveloper(true); }
        });
        devCard.addView(devGo);
        TextView devHint = Ui.tv(this, "Creador de AppHub: " + AppConfig.CREATOR_LOGIN,
                11, Ui.MUTED, false);
        Ui.margins(devHint, 0, 8, 0, 0, this);
        devCard.addView(devHint);
        p.box.addView(devCard);
        Ui.margins(devCard, 0, 0, 0, 16, this);

        p.box.addView(sectionTitle("Resultados"));
        searchHeading = Ui.tv(this, "", 12, Ui.MUTED, false);
        Ui.margins(searchHeading, 0, 4, 0, 8, this);
        p.box.addView(searchHeading);
        searchList = Ui.vertical(this);
        p.box.addView(searchList, rowParams());
        searchStatus = makeStatus();
        p.box.addView(searchStatus.view);
        return p;
    }

    private Page buildCreatorPage() {
        Page p = newPage();
        LinearLayout card = Ui.card(this);
        card.addView(Ui.tv(this, "@" + AppConfig.CREATOR_LOGIN, 17, Ui.TEXT, true));
        TextView body = Ui.tv(this,
                "Las apps que el creador de AppHub va publicando en sus repositorios de GitHub. Pulsa una para ver sus capturas y descargar su APK.",
                13, Ui.MUTED, false);
        Ui.margins(body, 0, 6, 0, 0, this);
        card.addView(body);
        TextView profile = Ui.secondaryButton(this, "Ver perfil en GitHub");
        Ui.margins(profile, 0, 12, 0, 0, this);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/" + AppConfig.CREATOR_LOGIN)));
            }
        });
        card.addView(profile);
        p.box.addView(card);
        Ui.margins(card, 0, 0, 0, 18, this);

        p.box.addView(sectionTitle("Apps publicadas"));
        creatorList = Ui.vertical(this);
        p.box.addView(creatorList, rowParams());
        creatorStatus = makeStatus();
        p.box.addView(creatorStatus.view);
        return p;
    }

    private Page buildMinePage() {
        Page p = newPage();
        mineCard = Ui.card(this);
        p.box.addView(mineCard);
        Ui.margins(mineCard, 0, 0, 0, 18, this);
        p.box.addView(sectionTitle("Tus repositorios"));
        mineList = Ui.vertical(this);
        p.box.addView(mineList, rowParams());
        mineStatus = makeStatus();
        p.box.addView(mineStatus.view);
        return p;
    }

    private Page buildUpdatesPage() {
        Page p = newPage();
        selfCard = Ui.card(this);
        p.box.addView(selfCard);
        Ui.margins(selfCard, 0, 0, 0, 20, this);

        LinearLayout head = Ui.horizontal(this);
        head.addView(sectionTitle("Apps instaladas desde AppHub"));
        TextView check = Ui.tv(this, "Comprobar", 13, Ui.PRIMARY, true);
        check.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
        check.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { checkInstalledUpdates(); }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER_VERTICAL;
        cp.leftMargin = Ui.dp(this, 8);
        check.setLayoutParams(cp);
        head.addView(check);
        p.box.addView(head);

        updatesAppList = Ui.vertical(this);
        p.box.addView(updatesAppList, rowParams());
        updatesStatus = makeStatus();
        p.box.addView(updatesStatus.view);
        return p;
    }

    private LinearLayout buildNav() {
        LinearLayout nav = Ui.horizontal(this);
        nav.setBackgroundColor(Ui.CARD);
        nav.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        for (int i = 0; i < 5; i++) {
            final int index = i;
            LinearLayout item = Ui.vertical(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            item.setLayoutParams(lp);
            ImageView iv = Ui.icon(this, NAV_ICONS[i], Ui.MUTED, 22);
            TextView label = Ui.tv(this, NAV_LABELS[i], 11, Ui.MUTED, false);
            label.setGravity(Gravity.CENTER);
            item.addView(iv);
            item.addView(label);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectTab(index); }
            });
            navItems[i] = item;
            navIcons[i] = iv;
            navLabels[i] = label;
            nav.addView(item);
        }
        return nav;
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextView sectionTitle(String text) {
        return Ui.tv(this, text, 15, Ui.TEXT, true);
    }

    private Status makeStatus() {
        Status s = new Status();
        s.view = Ui.vertical(this);
        s.text = Ui.tv(this, "", 13, Ui.MUTED, false);
        s.text.setGravity(Gravity.CENTER);
        s.text.setPadding(Ui.dp(this, 16), Ui.dp(this, 22), Ui.dp(this, 16), Ui.dp(this, 8));
        s.retry = Ui.secondaryButton(this, "Reintentar");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.gravity = Gravity.CENTER_HORIZONTAL;
        s.retry.setLayoutParams(rp);
        s.retry.setVisibility(View.GONE);
        s.view.addView(s.text);
        s.view.addView(s.retry);
        s.view.setVisibility(View.GONE);
        return s;
    }

    private void showStatus(Status s, String message, final Runnable retry) {
        s.text.setText(message);
        s.retry.setVisibility(retry == null ? View.GONE : View.VISIBLE);
        s.retry.setOnClickListener(retry == null ? null : new View.OnClickListener() {
            @Override public void onClick(View v) { retry.run(); }
        });
        s.view.setVisibility(View.VISIBLE);
    }

    private void hideStatus(Status s) { s.view.setVisibility(View.GONE); }

    private void selectTab(int index) {
        tab = index;
        for (int i = 0; i < 5; i++) {
            pages[i].scroll.setVisibility(i == index ? View.VISIBLE : View.GONE);
            boolean active = i == index;
            navIcons[i].setImageTintList(android.content.res.ColorStateList.valueOf(
                    active ? Ui.PRIMARY : Ui.MUTED));
            navLabels[i].setTextColor(active ? Ui.PRIMARY : Ui.MUTED);
            navLabels[i].setTypeface(android.graphics.Typeface.DEFAULT,
                    active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            navItems[i].setBackground(active ? Ui.rounded(0xFFE8F0FF, 12) : null);
        }
        if (index == TAB_EXPLORE && !exploreLoaded) loadExplore(false);
        if (index == TAB_CREATOR) loadCreator();
        if (index == TAB_MINE) loadMine();
        if (index == TAB_UPDATES) loadUpdates();
    }

    private void refreshTab() {
        if (tab == TAB_EXPLORE) {
            explorePage++;
            loadExplore(true);
        } else if (tab == TAB_SEARCH) loadSearch(true);
        else if (tab == TAB_CREATOR) loadCreator();
        else if (tab == TAB_MINE) loadMine();
        else loadUpdates();
    }

    private void loadExplore(final boolean force) {
        showStatus(exploreStatus, "Buscando apps en GitHub…", null);
        GitHubApi.search("topic:android-app topic:apk", explorePage, force,
                new GitHubApi.Callback<List<AppInfo>>() {
                    @Override public void onResult(List<AppInfo> list, Throwable error) {
                        if (isFinishing() || isDestroyed()) return;
                        if (error != null) {
                            showStatus(exploreStatus, message(error), new Runnable() {
                                @Override public void run() { loadExplore(true); }
                            });
                            return;
                        }
                        if (list == null || list.isEmpty()) {
                            showStatus(exploreStatus,
                                    "No hay más apps en esta página. Toca actualizar para seguir explorando.", null);
                            return;
                        }
                        exploreLoaded = true;
                        allExploreApps.clear();
                        for (AppInfo app : list) if (app != null) allExploreApps.add(app);
                        renderCategories();
                        renderExplore();
                    }
                });
    }

    private void renderCategories() {
        categoryStrip.removeAllViews();
        for (final String category : Categories.NAMES) {
            boolean active = category.equals(selectedCategory);
            TextView chip = Ui.tv(this, category, 12, active ? 0xFFFFFFFF : Ui.MUTED, true);
            chip.setBackground(active ? Ui.rounded(Ui.PRIMARY, 16) : Ui.rounded(Ui.CARD, 16));
            if (!active) {
                android.graphics.drawable.GradientDrawable d = Ui.rounded(Ui.CARD, 16);
                d.setStroke(Ui.dp(this, 1), Ui.LINE);
                chip.setBackground(d);
            }
            chip.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.rightMargin = Ui.dp(this, 6);
            chip.setLayoutParams(cp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectedCategory = category;
                    renderCategories();
                    renderExplore();
                }
            });
            categoryStrip.addView(chip);
        }
    }

    private void renderExplore() {
        exploreList.removeAllViews();
        versionLabels.clear();
        photoLabels.clear();
        List<AppInfo> filtered = new ArrayList<AppInfo>();
        for (AppInfo app : allExploreApps) {
            if (Categories.ALL.equals(selectedCategory) || selectedCategory.equals(app.category)) {
                filtered.add(app);
            }
        }
        if (filtered.isEmpty()) {
            showStatus(exploreStatus, "No hay apps en «" + selectedCategory + "». Prueba otra categoría.", null);
            return;
        }
        hideStatus(exploreStatus);
        int limit = Math.min(8, filtered.size());
        for (int i = 0; i < limit; i++) addAppRow(exploreList, filtered.get(i), true);
        for (int i = 0; i < limit; i++) {
            loadVersionBadge(filtered.get(i));
            loadPhotoBadge(filtered.get(i));
        }
    }

    private void loadSearch(final boolean force) {
        String q = searchField.getText().toString().trim();
        if (q.length() == 0) {
            Toast.makeText(this, "Escribe algo para buscar", Toast.LENGTH_SHORT).show();
            return;
        }
        searchHeading.setText("Resultados para «" + q + "»");
        showStatus(searchStatus, "Buscando «" + q + "» en GitHub…", null);
        GitHubApi.search(q, force, new GitHubApi.Callback<List<AppInfo>>() {
            @Override public void onResult(List<AppInfo> list, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                searchList.removeAllViews();
                if (error != null) {
                    showStatus(searchStatus, message(error), new Runnable() {
                        @Override public void run() { loadSearch(true); }
                    });
                    return;
                }
                if (list == null || list.isEmpty()) {
                    showStatus(searchStatus, "Sin resultados. Prueba con otras palabras.", null);
                    return;
                }
                hideStatus(searchStatus);
                for (AppInfo app : list) addAppRow(searchList, app, false);
            }
        });
    }

    private void loadDeveloper(final boolean force) {
        String login = developerField.getText().toString().trim().replace("@", "");
        if (login.length() == 0) {
            Toast.makeText(this, "Escribe un usuario de GitHub", Toast.LENGTH_SHORT).show();
            return;
        }
        searchHeading.setText("Desarrollador: @" + login);
        showStatus(searchStatus, "Buscando apps de @" + login + "…", null);
        GitHubApi.userPublicRepos(login, force, new GitHubApi.Callback<List<AppInfo>>() {
            @Override public void onResult(List<AppInfo> list, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                searchList.removeAllViews();
                if (error != null) {
                    showStatus(searchStatus, message(error), new Runnable() {
                        @Override public void run() { loadDeveloper(true); }
                    });
                    return;
                }
                if (list == null || list.isEmpty()) {
                    showStatus(searchStatus, "Ese usuario no tiene repositorios públicos.", null);
                    return;
                }
                hideStatus(searchStatus);
                for (AppInfo app : list) if (app != null) addAppRow(searchList, app, false);
            }
        });
    }

    private void loadCreator() {
        showStatus(creatorStatus, "Cargando apps de @" + AppConfig.CREATOR_LOGIN + "…", null);
        GitHubApi.userPublicRepos(AppConfig.CREATOR_LOGIN, false, new GitHubApi.Callback<List<AppInfo>>() {
            @Override public void onResult(List<AppInfo> list, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                creatorList.removeAllViews();
                if (error != null) {
                    showStatus(creatorStatus, message(error), new Runnable() {
                        @Override public void run() { loadCreator(); }
                    });
                    return;
                }
                if (list == null || list.isEmpty()) {
                    showStatus(creatorStatus, "Todavía no hay apps publicadas por el creador.", null);
                    return;
                }
                hideStatus(creatorStatus);
                for (AppInfo app : list) if (app != null) addAppRow(creatorList, app, false);
            }
        });
    }

    private void loadVersionBadge(final AppInfo app) {
        GitHubApi.latestRelease(app.fullName, false, new GitHubApi.Callback<ReleaseInfo>() {
            @Override public void onResult(ReleaseInfo release, Throwable error) {
                TextView label = versionLabels.get(app.fullName);
                if (label == null || isFinishing()) return;
                if (release != null && release.hasApk && release.tag != null && release.tag.length() > 0) {
                    label.setText(release.tag);
                    label.setTextColor(Ui.PRIMARY);
                } else {
                    label.setText("sin APK");
                    label.setTextColor(Ui.MUTED);
                }
            }
        });
    }

    private void loadPhotoBadge(final AppInfo app) {
        ReadmeInfo cached = GitHubApi.cachedReadme(app.fullName);
        if (cached != null) {
            applyPhotoBadge(app.fullName, cached.images.size());
            return;
        }
        GitHubApi.readme(app.fullName, false, new GitHubApi.Callback<ReadmeInfo>() {
            @Override public void onResult(ReadmeInfo info, Throwable error) {
                applyPhotoBadge(app.fullName, info == null ? 0 : info.images.size());
            }
        });
    }

    private void applyPhotoBadge(String repo, int count) {
        TextView label = photoLabels.get(repo);
        if (label == null || count <= 0) return;
        label.setText("fotos " + count);
        label.setVisibility(View.VISIBLE);
    }

    private void addAppRow(LinearLayout container, final AppInfo app, boolean withVersion) {
        LinearLayout row = Ui.horizontal(this);
        GradientDrawable bg = Ui.rounded(Ui.CARD, 14);
        bg.setStroke(Ui.dp(this, 1), Ui.LINE);
        row.setBackground(bg);
        row.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        row.setElevation(Ui.dp(this, 1));

        ImageView avatar = new ImageView(this);
        int size = Ui.dp(this, 46);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Ui.dp(MainActivity.this, 10));
            }
        });
        avatar.setClipToOutline(true);
        AvatarLoader.load(avatar, app.avatarUrl, app.displayName());
        row.addView(avatar);

        LinearLayout info = Ui.vertical(this);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        ip.leftMargin = Ui.dp(this, 12);
        info.setLayoutParams(ip);
        info.addView(Ui.tv(this, app.displayName(), 15, Ui.TEXT, true));
        TextView desc = Ui.tv(this, app.description, 12, Ui.MUTED, false);
        desc.setMaxLines(2);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        Ui.margins(desc, 0, 2, 0, 0, this);
        info.addView(desc);

        LinearLayout meta = Ui.horizontal(this);
        Ui.margins(meta, 0, 6, 0, 0, this);
        meta.addView(Ui.tv(this, app.owner, 11, Ui.MUTED, false));
        meta.addView(Ui.tv(this, "  ★ " + app.stars, 11, Ui.MUTED, false));
        if (withVersion) {
            TextView badge = Ui.tv(this, "…", 10, Ui.PRIMARY, true);
            badge.setBackground(Ui.rounded(0xFFE8F0FF, 8));
            badge.setPadding(Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.leftMargin = Ui.dp(this, 8);
            badge.setLayoutParams(bp);
            meta.addView(badge);
            versionLabels.put(app.fullName, badge);

            TextView photos = Ui.tv(this, "", 10, Ui.GREEN, true);
            photos.setBackground(Ui.rounded(Ui.GREEN_BG, 8));
            photos.setPadding(Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pp.leftMargin = Ui.dp(this, 6);
            photos.setLayoutParams(pp);
            photos.setVisibility(View.GONE);
            meta.addView(photos);
            photoLabels.put(app.fullName, photos);
        }
        info.addView(meta);
        row.addView(info);

        ImageView action = Ui.icon(this, R.drawable.ic_download, Ui.PRIMARY, 20);
        action.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8));
        action.setContentDescription("Ver app");
        row.addView(action);

        View.OnClickListener open = new View.OnClickListener() {
            @Override public void onClick(View v) { openDetail(app); }
        };
        row.setOnClickListener(open);
        action.setOnClickListener(open);
        LinearLayout.LayoutParams lp = rowParams();
        lp.bottomMargin = Ui.dp(this, 8);
        row.setLayoutParams(lp);
        container.addView(row);
    }

    private void openDetail(AppInfo app) {
        Intent i = new Intent(this, AppDetailActivity.class);
        i.putExtra("app", app);
        startActivity(i);
    }

    private void loadMine() {
        mineCard.removeAllViews();
        mineCard.addView(Ui.tv(this, "Tus apps en GitHub", 16, Ui.TEXT, true));
        if (!StorePrefs.hasToken()) {
            TextView body = Ui.tv(this, "Conecta tu cuenta para ver tus repositorios y las apps que publicas. El token se guarda solo en este dispositivo.",
                    13, Ui.MUTED, false);
            Ui.margins(body, 0, 6, 0, 0, this);
            mineCard.addView(body);
            TextView connect = Ui.primaryButton(this, "Conectar GitHub");
            Ui.margins(connect, 0, 14, 0, 0, this);
            connect.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, TokenActivity.class));
                }
            });
            mineCard.addView(connect);
            TextView create = Ui.tv(this, "¿No tienes un token? Crear uno en GitHub →", 12, Ui.PRIMARY, false);
            Ui.margins(create, 0, 10, 0, 0, this);
            create.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/settings/tokens")));
                }
            });
            mineCard.addView(create);
            showStatus(mineStatus, "Conecta tu GitHub para ver tus apps aquí.", null);
            mineList.removeAllViews();
            return;
        }

        String token = StorePrefs.token();
        String tail = token.length() > 4 ? token.substring(token.length() - 4) : "••••";
        mineCard.addView(Ui.tv(this, "Conectado · token ••••" + tail, 12, Ui.GREEN, false));
        LinearLayout actions = Ui.horizontal(this);
        Ui.margins(actions, 0, 12, 0, 0, this);
        TextView change = Ui.secondaryButton(this, "Cambiar token");
        change.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TokenActivity.class));
            }
        });
        TextView logout = Ui.secondaryButton(this, "Desconectar");
        Ui.margins(logout, 8, 0, 0, 0, this);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("¿Desconectar GitHub?")
                        .setMessage("El token se borrará de este dispositivo.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Desconectar", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int which) {
                                StorePrefs.setToken("");
                                loadMine();
                                Toast.makeText(MainActivity.this, "GitHub desconectado", Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            }
        });
        actions.addView(change);
        actions.addView(logout);
        mineCard.addView(actions);

        showStatus(mineStatus, "Cargando tus repositorios…", null);
        GitHubApi.userRepos(false, new GitHubApi.Callback<List<AppInfo>>() {
            @Override public void onResult(List<AppInfo> list, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                mineList.removeAllViews();
                if (error != null) {
                    showStatus(mineStatus, message(error), new Runnable() {
                        @Override public void run() { loadMine(); }
                    });
                    return;
                }
                if (list == null || list.isEmpty()) {
                    showStatus(mineStatus, "Tu cuenta no tiene repositorios visibles.", null);
                    return;
                }
                hideStatus(mineStatus);
                for (AppInfo app : list) addAppRow(mineList, app, false);
            }
        });
    }

    private void loadUpdates() {
        PackageManager pm = getPackageManager();
        String version = "?";
        try { version = pm.getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
        final String currentVersion = version;
        selfCard.removeAllViews();
        selfCard.addView(Ui.tv(this, "AppHub · versión " + currentVersion, 16, Ui.TEXT, true));
        selfResult = Ui.tv(this, "", 13, Ui.MUTED, false);
        Ui.margins(selfResult, 0, 6, 0, 0, this);
        selfCard.addView(selfResult);
        selfDownload = null;

        final String repo = StorePrefs.selfRepo();
        if (repo == null) {
            selfResult.setText("Sin repositorio configurado. Cuando publiques AppHub en GitHub, indícalo aquí para recibir actualizaciones.");
            TextView configure = Ui.secondaryButton(this, "Configurar repositorio");
            Ui.margins(configure, 0, 12, 0, 0, this);
            configure.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SelfRepoActivity.class));
                }
            });
            selfCard.addView(configure);
        } else {
            selfResult.setText("Repositorio: " + repo);
            LinearLayout actions = Ui.horizontal(this);
            Ui.margins(actions, 0, 12, 0, 0, this);
            TextView check = Ui.primaryButton(this, "Comprobar");
            check.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { checkSelfUpdate(repo, currentVersion); }
            });
            actions.addView(check);
            TextView edit = Ui.secondaryButton(this, "Editar repo");
            Ui.margins(edit, 8, 0, 0, 0, this);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SelfRepoActivity.class));
                }
            });
            actions.addView(edit);
            selfCard.addView(actions);
        }

        renderInstalledApps();
    }

    private void checkSelfUpdate(final String repo, final String currentVersion) {
        selfResult.setText("Consultando GitHub…");
        GitHubApi.latestRelease(repo, true, new GitHubApi.Callback<ReleaseInfo>() {
            @Override public void onResult(ReleaseInfo release, Throwable error) {
                if (isFinishing()) return;
                if (error != null) { selfResult.setText(message(error)); return; }
                if (release == null || !release.hasApk) {
                    selfResult.setText("El último release no contiene un APK para este dispositivo.");
                    return;
                }
                if (!Versions.isNewer(release.tag, currentVersion)) {
                    selfResult.setText("Estás al día. Última versión en GitHub: " + release.tag);
                    return;
                }
                selfResult.setText("Hay una versión nueva: " + release.tag + " · " + readableSize(release.apkSize));
                selfResult.setTextColor(Ui.GREEN);
                if (selfDownload == null) {
                    selfDownload = Ui.primaryButton(MainActivity.this, "Descargar actualización");
                    Ui.margins(selfDownload, 0, 12, 0, 0, MainActivity.this);
                    selfCard.addView(selfDownload);
                }
                final AppInfo self = new AppInfo(repo, repo.split("/")[0], "AppHub",
                        "Actualización de AppHub", "https://github.com/" + repo, "", 0);
                selfDownload.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { startDownload(self, release); }
                });
            }
        });
    }

    private void renderInstalledApps() {
        updateLabels.clear();
        updatesAppList.removeAllViews();
        List<StorePrefs.Tracked> tracked = StorePrefs.allTracked();
        if (tracked.isEmpty()) {
            showStatus(updatesStatus, "Aún no has instalado apps desde AppHub. Descarga una para seguir sus actualizaciones.", null);
            return;
        }
        hideStatus(updatesStatus);
        for (StorePrefs.Tracked item : tracked) {
            String shortName = item.repo.contains("/")
                    ? item.repo.substring(item.repo.indexOf('/') + 1) : item.repo;
            LinearLayout row = Ui.card(this);
            LinearLayout.LayoutParams lp = rowParams();
            lp.bottomMargin = Ui.dp(this, 8);
            row.setLayoutParams(lp);
            row.addView(Ui.tv(this, shortName, 14, Ui.TEXT, true));
            row.addView(Ui.tv(this, item.repo, 11, Ui.MUTED, false));
            TextView state = Ui.tv(this, "Instalada: " + item.version, 12, Ui.MUTED, false);
            Ui.margins(state, 0, 6, 0, 0, this);
            row.addView(state);
            updateLabels.put(item.repo, state);
            updatesAppList.addView(row);
        }
    }

    private void checkInstalledUpdates() {
        List<StorePrefs.Tracked> tracked = StorePrefs.allTracked();
        if (tracked.isEmpty()) {
            Toast.makeText(this, "Todavía no hay apps instaladas desde AppHub", Toast.LENGTH_SHORT).show();
            return;
        }
        for (final StorePrefs.Tracked item : tracked) {
            final TextView label = updateLabels.get(item.repo);
            if (label == null) continue;
            label.setText("Comprobando…");
            GitHubApi.latestRelease(item.repo, true, new GitHubApi.Callback<ReleaseInfo>() {
                @Override public void onResult(ReleaseInfo release, Throwable error) {
                    if (isFinishing() || label.getParent() == null) return;
                    if (error != null) { label.setText("No se pudo comprobar"); return; }
                    if (release == null || !release.hasApk) { label.setText("Sin APK en el release"); return; }
                    if (Versions.isNewer(release.tag, item.version)) {
                        label.setText("Actualización disponible: " + release.tag + " · toca para descargar");
                        label.setTextColor(Ui.GREEN);
                        final AppInfo app = new AppInfo(item.repo,
                                item.repo.split("/")[0], item.repo.split("/")[1], "",
                                "https://github.com/" + item.repo, "", 0);
                        label.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) { startDownload(app, release); }
                        });
                    } else {
                        label.setText("Al día · " + release.tag);
                        label.setTextColor(Ui.MUTED);
                        label.setOnClickListener(null);
                    }
                }
            });
        }
    }

    private void startDownload(AppInfo app, ReleaseInfo release) {
        if (!InstallHelper.canInstall(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permitir instalar apps")
                    .setMessage("Android necesita que actives «Instalar apps desconocidas» para AppHub. Te llevo a Ajustes.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Ir a Ajustes", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int which) {
                            InstallHelper.openInstallSettings(MainActivity.this);
                        }
                    }).show();
            return;
        }
        long id = DownloadLauncher.enqueue(this, app, release);
        if (id == -1) Toast.makeText(this, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Descarga iniciada · toca la notificación para instalar", Toast.LENGTH_LONG).show();
    }

    private static String message(Throwable error) {
        if (error == null) return "Error desconocido";
        String m = error.getMessage();
        return (m == null || m.length() == 0) ? "No se pudo completar la acción." : m;
    }

    private static String readableSize(long bytes) {
        if (bytes <= 0) return "tamaño desconocido";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024f) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }
}
