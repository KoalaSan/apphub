package com.buddyapps.apphub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Carga avatares de GitHub y muestra una inicial como respaldo sin red. */
public final class AvatarLoader {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(80);

    private AvatarLoader() { }

    public static void load(final ImageView view, final String url, String name) {
        String letter = (name == null || name.length() == 0) ? "?" : name.substring(0, 1).toUpperCase();
        view.setImageDrawable(new LetterDrawable(letter, colorFor(name)));
        if (url == null || url.length() == 0) return;
        view.setTag(url);
        Bitmap hit = CACHE.get(url);
        if (hit != null) { view.setImageBitmap(hit); return; }
        POOL.execute(new Runnable() {
            @Override public void run() {
                Bitmap bitmap = download(url);
                if (bitmap == null) return;
                CACHE.put(url, bitmap);
                MAIN.post(new Runnable() {
                    @Override public void run() {
                        if (url.equals(view.getTag())) view.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private static Bitmap download(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "AppHub/1.0 (Android)");
            if (conn.getResponseCode() != 200) return null;
            InputStream in = conn.getInputStream();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            Bitmap raw = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            if (raw == null) return null;
            return Bitmap.createScaledBitmap(raw, 96, 96, true);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static int colorFor(String s) {
        int h = 0;
        if (s != null) for (int i = 0; i < s.length(); i++) h = h * 31 + s.charAt(i);
        int[] palette = {0xFF2F6DF6, 0xFF7C3AED, 0xFF0E9384, 0xFFDB2777, 0xFFEA580C, 0xFF2563EB};
        return palette[Math.abs(h) % palette.length];
    }

    private static final class LetterDrawable extends Drawable {
        private final String letter;
        private final int color;
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);

        LetterDrawable(String letter, int color) {
            this.letter = letter;
            this.color = color;
            fg.setColor(Color.WHITE);
            fg.setTextAlign(Paint.Align.CENTER);
            fg.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float cx = b.centerX(), cy = b.centerY();
            float r = Math.min(b.width(), b.height()) / 2f;
            bg.setColor(color);
            canvas.drawCircle(cx, cy, r, bg);
            fg.setTextSize(r * 1.25f);
            canvas.drawText(letter, cx, cy - (fg.descent() + fg.ascent()) / 2f, fg);
        }

        @Override public void setAlpha(int alpha) { bg.setAlpha(alpha); fg.setAlpha(alpha); }

        @Override public void setColorFilter(android.graphics.ColorFilter cf) {
            fg.setColorFilter(cf);
        }

        @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
    }
}
