package com.buddyapps.apphub;

import java.util.List;
import java.util.Locale;

/** Clasificación local de repositorios por temas y palabras de su descripción. */
public final class Categories {
    public static final String ALL = "Todas";
    public static final String[] NAMES = {ALL, "Herramientas", "Productividad", "Juegos",
            "Educación", "Desarrollo", "Personalización", "Entretenimiento", "Otros"};

    private Categories() { }

    public static String classify(AppInfo app) {
        StringBuilder sb = new StringBuilder();
        sb.append(app.name).append(' ').append(app.description == null ? "" : app.description);
        if (app.topics != null) {
            for (String topic : app.topics) sb.append(' ').append(topic);
        }
        String text = sb.toString().toLowerCase(Locale.US);
        if (has(text, "game", "juego", "puzzle", "arcade", "emulator", "retro", "rpg")) return "Juegos";
        if (has(text, "education", "learn", "study", "dictionary", "translate", "course",
                "science", "math", "book", "reader")) return "Educación";
        if (has(text, "developer", "development", "ide", "code editor", "git", "terminal",
                "debug", "sdk", "compiler", "programming")) return "Desarrollo";
        if (has(text, "productivity", "todo", "notes", "note taking", "task", "calendar",
                "markdown", "clipboard", "keyboard", "file manager", "office")) return "Productividad";
        if (has(text, "utility", "toolkit", "tools", "scanner", "converter", "downloader",
                "cleaner", "backup", "browser", "manager", "launcher")) return "Herramientas";
        if (has(text, "theme", "wallpaper", "icon pack", "widget", "customize", "skin",
                "personalization")) return "Personalización";
        if (has(text, "music", "video", "media", "player", "streaming", "podcast", "tv",
                "anime", "gallery", "camera", "photo")) return "Entretenimiento";
        return "Otros";
    }

    private static boolean has(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }
}
