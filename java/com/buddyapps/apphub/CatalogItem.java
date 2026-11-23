package com.buddyapps.apphub;

import java.io.Serializable;

/** App publicada desde AppHub en el repositorio de catálogo del creador. */
public final class CatalogItem implements Serializable {
    public final String name;
    public final String owner;
    public final String repo;
    public final String description;
    public final String category;
    public final String version;
    public final String apkName;
    public final String apkUrl;
    public final String releaseUrl;

    public CatalogItem(String name, String owner, String repo, String description, String category,
                       String version, String apkName, String apkUrl, String releaseUrl) {
        this.name = name;
        this.owner = owner;
        this.repo = repo;
        this.description = description;
        this.category = category;
        this.version = version;
        this.apkName = apkName;
        this.apkUrl = apkUrl;
        this.releaseUrl = releaseUrl;
    }
}
