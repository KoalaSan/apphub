package com.buddyapps.apphub;

/** Valores fijos de la tienda: quién publica las apps y dónde se actualiza AppHub. */
public final class AppConfig {
    public static final String CREATOR_LOGIN = "KoalaSan";
    public static final String SELF_REPO = "KoalaSan/apphub";
    /** Repositorio donde se guarda el catálogo de apps publicadas desde la propia app. */
    public static final String CATALOG_REPO = "KoalaSan/apphub";
    public static final String CATALOG_URL =
            "https://raw.githubusercontent.com/KoalaSan/apphub/main/catalog.json";

    private AppConfig() { }
}
