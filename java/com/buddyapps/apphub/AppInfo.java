package com.buddyapps.apphub;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Datos de un repositorio de GitHub que puede contener una app. */
public final class AppInfo implements Serializable {
    public final String fullName;
    public final String owner;
    public final String name;
    public final String description;
    public final String htmlUrl;
    public final String avatarUrl;
    public final int stars;
    public final List<String> topics;
    public String category = Categories.ALL;

    public AppInfo(String fullName, String owner, String name, String description,
                   String htmlUrl, String avatarUrl, int stars) {
        this(fullName, owner, name, description, htmlUrl, avatarUrl, stars, new ArrayList<String>());
    }

    public AppInfo(String fullName, String owner, String name, String description,
                   String htmlUrl, String avatarUrl, int stars, List<String> topics) {
        this.fullName = fullName;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.htmlUrl = htmlUrl;
        this.avatarUrl = avatarUrl;
        this.stars = stars;
        this.topics = topics;
    }

    public String displayName() {
        return (name == null || name.length() == 0) ? fullName : name;
    }
}
