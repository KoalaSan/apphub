package com.buddyapps.apphub;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Metadatos extra del repositorio: licencia, lenguaje, temas y enlaces. */
public final class RepoMeta implements Serializable {
    public final String license;      // nombre legible de la licencia
    public final String licenseSpdx;  // identificador, p. ej. MIT
    public final String licenseUrl;
    public final String language;
    public final String homepage;
    public final int forks;
    public final int issues;
    public final String pushedAt;
    public final List<String> topics;

    public RepoMeta(String license, String licenseSpdx, String licenseUrl, String language,
                    String homepage, int forks, int issues, String pushedAt, List<String> topics) {
        this.license = license == null ? "" : license;
        this.licenseSpdx = licenseSpdx == null ? "" : licenseSpdx;
        this.licenseUrl = licenseUrl == null ? "" : licenseUrl;
        this.language = language == null ? "" : language;
        this.homepage = homepage == null ? "" : homepage;
        this.forks = forks;
        this.issues = issues;
        this.pushedAt = pushedAt == null ? "" : pushedAt;
        this.topics = topics == null ? new ArrayList<String>() : topics;
    }

    public boolean hasLicense() {
        return license.length() > 0 || licenseSpdx.length() > 0;
    }

    public String licenseLabel() {
        if (licenseSpdx.length() > 0 && !"NOASSERTION".equals(licenseSpdx)) return licenseSpdx;
        return license;
    }
}
