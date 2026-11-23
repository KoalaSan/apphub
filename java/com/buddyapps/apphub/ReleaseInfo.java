package com.buddyapps.apphub;

import java.io.Serializable;

/** Release de GitHub y el APK elegido para este dispositivo. */
public final class ReleaseInfo implements Serializable {
    public final String tag;
    public final String publishedAt;
    public final String apkName;
    public final String apkUrl;
    public final long apkSize;
    public final boolean hasApk;

    public ReleaseInfo(String tag, String publishedAt, String apkName,
                       String apkUrl, long apkSize, boolean hasApk) {
        this.tag = tag;
        this.publishedAt = publishedAt;
        this.apkName = apkName;
        this.apkUrl = apkUrl;
        this.apkSize = apkSize;
        this.hasApk = hasApk;
    }
}
