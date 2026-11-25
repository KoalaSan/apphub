package com.buddyapps.apphub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Carga capturas de pantalla del README recortadas al tamaño de la tarjeta. */
public final class RemoteImageLoader {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(30);

    private RemoteImageLoader() { }

    /** Resultado de una carga a pantalla completa. */
    public interface FullCallback {
        void onLoaded();
        void onError();
    }

    /** Carga la imagen completa, sin recortar, para el visor a pantalla completa. */
    public static void loadFull(final ImageView view, final String url, final FullCallback cb) {
        final String key = url + "|full";
        view.setTag(key);
        POOL.execute(new Runnable() {
            @Override public void run() {
                Bitmap hit = CACHE.get(key);
                if (hit == null) {
                    hit = downloadFull(url);
                    if (hit == null) {
                        if (cb != null) MAIN.post(new Runnable() {
                            @Override public void run() { cb.onError(); }
                        });
                        return;
                    }
                    CACHE.put(key, hit);
                }
                final Bitmap bitmap = hit;
                MAIN.post(new Runnable() {
                    @Override public void run() {
                        if (key.equals(view.getTag())) view.setImageBitmap(bitmap);
                        if (cb != null) cb.onLoaded();
                    }
                });
            }
        });
    }

    /** Decodifica limitando el lado mayor a 2400 px para no agotar la memoria. */
    private static Bitmap downloadFull(String url) {
        HttpURLConnection conn = open(url);
        if (conn == null) return null;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        try {
            InputStream in = conn.getInputStream();
            BitmapFactory.decodeStream(in, null, opt);
            in.close();
        } catch (Exception ignored) {
        } finally {
            conn.disconnect();
        }
        int max = Math.max(opt.outWidth, opt.outHeight);
        int sample = 1;
        while (max / sample > 2400) sample *= 2;
        conn = open(url);
        if (conn == null) return null;
        try {
            BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = sample;
            InputStream in = conn.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, decode);
            in.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        } finally {
            conn.disconnect();
        }
    }

    private static HttpURLConnection open(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "AppHub/1.0 (Android)");
            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return null;
            }
            return conn;
        } catch (Exception e) {
            if (conn != null) conn.disconnect();
            return null;
        }
    }

    public static void load(final ImageView view, final String url, final int width, final int height) {
        String key = url + "|" + width + "x" + height;
        view.setTag(key);
        view.setBackgroundColor(Ui.LINE);
        Bitmap hit = CACHE.get(key);
        if (hit != null) { view.setImageBitmap(hit); return; }
        POOL.execute(new Runnable() {
            @Override public void run() {
                Bitmap bitmap = download(url, width, height);
                if (bitmap == null) return;
                CACHE.put(url + "|" + width + "x" + height, bitmap);
                MAIN.post(new Runnable() {
                    @Override public void run() {
                        if (key.equals(view.getTag())) view.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private static Bitmap download(String url, int width, int height) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "AppHub/1.0 (Android)");
            if (conn.getResponseCode() != 200) return null;
            InputStream in = conn.getInputStream();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            Bitmap raw = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            if (raw == null) return null;
            float scale = Math.max((float) width / raw.getWidth(), (float) height / raw.getHeight());
            int nw = Math.max(1, Math.round(raw.getWidth() * scale));
            int nh = Math.max(1, Math.round(raw.getHeight() * scale));
            Bitmap scaled = Bitmap.createScaledBitmap(raw, nw, nh, true);
            int x = Math.max(0, (nw - width) / 2);
            int y = Math.max(0, (nh - height) / 2);
            return Bitmap.createBitmap(scaled, x, y, Math.min(width, nw - x), Math.min(height, nh - y));
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
