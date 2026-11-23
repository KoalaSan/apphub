package com.buddyapps.apphub;

/** Comparación sencilla de versiones tipo v1.2.3 / 1.10 / 2.4-beta. */
public final class Versions {
    private Versions() { }

    public static boolean isNewer(String candidate, String installed) {
        int r = compare(candidate, installed);
        return r > 0;
    }

    public static int compare(String a, String b) {
        String[] pa = parts(a), pb = parts(b);
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            String x = i < pa.length ? pa[i] : "0";
            String y = i < pb.length ? pb[i] : "0";
            try {
                int ix = Integer.parseInt(x), iy = Integer.parseInt(y);
                if (ix != iy) return ix > iy ? 1 : -1;
            } catch (NumberFormatException e) {
                int c = x.compareToIgnoreCase(y);
                if (c != 0) return c > 0 ? 1 : -1;
            }
        }
        return 0;
    }

    private static String[] parts(String v) {
        if (v == null) return new String[0];
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        return s.split("[^0-9A-Za-z]+");
    }
}
