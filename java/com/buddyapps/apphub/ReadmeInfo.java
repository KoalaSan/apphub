package com.buddyapps.apphub;

import java.io.Serializable;
import java.util.List;

/** Datos útiles extraídos del README de un repositorio. */
public final class ReadmeInfo implements Serializable {
    public final List<String> images;
    public final String intro;

    public ReadmeInfo(List<String> images, String intro) {
        this.images = images;
        this.intro = intro;
    }
}
