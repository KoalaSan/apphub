package com.buddyapps.apphub;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Visor de capturas 100% dentro de la app, con zoom y arrastre. */
public class ImageViewerActivity extends Activity {
    private ZoomImage image;
    private ProgressBar loading;
    private TextView error;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) { finish(); return; }

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(0xFF0E1420);
        root.setFitsSystemWindows(true);

        LinearLayout bar = Ui.horizontal(this);
        bar.setBackgroundColor(0xFF151D2C);
        bar.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10));
        TextView close = Ui.tv(this, "✕  Cerrar", 15, 0xFFFFFFFF, true);
        close.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        bar.addView(close);

        TextView hint = Ui.tv(this, "Pellizca para ampliar · arrastra para mover",
                12, 0xFF9AA8BF, false);
        hint.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        hint.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 8), 0);
        bar.addView(hint);

        TextView fit = Ui.tv(this, "Ajustar", 14, 0xFF8FB4FF, true);
        fit.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        fit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { image.fit(); }
        });
        bar.addView(fit);
        root.addView(bar);

        FrameLayout stage = new FrameLayout(this);
        image = new ZoomImage(this);
        stage.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loading = new ProgressBar(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 44));
        lp.gravity = android.view.Gravity.CENTER;
        loading.setLayoutParams(lp);
        stage.addView(loading);
        error = Ui.tv(this, "No se pudo cargar la imagen", 14, 0xFFC6D0E0, false);
        error.setVisibility(View.GONE);
        error.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        ep.gravity = android.view.Gravity.CENTER;
        error.setLayoutParams(ep);
        stage.addView(error);
        root.addView(stage, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        RemoteImageLoader.loadFull(image, url, new RemoteImageLoader.FullCallback() {
            @Override public void onLoaded() {
                loading.setVisibility(View.GONE);
                Anim.fadeIn(image);
            }
            @Override public void onError() {
                loading.setVisibility(View.GONE);
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    /** ImageView con matriz de zoom: ajusta al abrir, pellizca y arrastra sin librerías. */
    private static final class ZoomImage extends ImageView {
        private final Matrix matrix = new Matrix();
        private final Matrix saved = new Matrix();
        private final PointF last = new PointF();
        private final PointF lastMid = new PointF();
        private float lastDistance = 1f;
        private float baseScale = 1f;
        private Bitmap bitmap;

        ZoomImage(Context c) {
            super(c);
            setScaleType(ScaleType.MATRIX);
            setOnTouchListener(new Touch());
        }

        @Override public void setImageBitmap(Bitmap bm) {
            super.setImageBitmap(bm);
            bitmap = bm;
            fit();
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (bitmap != null) fit();
        }

        /** Centra la imagen completa dentro del area visible. */
        void fit() {
            if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
            float s = Math.min((float) getWidth() / bitmap.getWidth(),
                    (float) getHeight() / bitmap.getHeight());
            if (s > 1f) s = 1f;
            baseScale = s;
            matrix.reset();
            matrix.postScale(s, s);
            matrix.postTranslate((getWidth() - bitmap.getWidth() * s) / 2f,
                    (getHeight() - bitmap.getHeight() * s) / 2f);
            setImageMatrix(matrix);
        }

        private float scaleOf(Matrix m) {
            float[] values = new float[9];
            m.getValues(values);
            return values[Matrix.MSCALE_X];
        }

        private static float distance(MotionEvent e) {
            float dx = e.getX(0) - e.getX(1);
            float dy = e.getY(0) - e.getY(1);
            return Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        }

        private static void midpoint(MotionEvent e, PointF out) {
            out.set((e.getX(0) + e.getX(1)) / 2f, (e.getY(0) + e.getY(1)) / 2f);
        }

        private final class Touch implements View.OnTouchListener {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        saved.set(matrix);
                        last.set(e.getX(), e.getY());
                        return true;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        saved.set(matrix);
                        lastDistance = distance(e);
                        midpoint(e, lastMid);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (e.getPointerCount() >= 2) {
                            float wanted = scaleOf(saved) * distance(e) / lastDistance;
                            float max = baseScale * 8f;
                            float min = baseScale * 0.8f;
                            float scale = Math.max(min, Math.min(max, wanted)) / scaleOf(saved);
                            PointF mid = new PointF();
                            midpoint(e, mid);
                            matrix.set(saved);
                            matrix.postScale(scale, scale, mid.x, mid.y);
                            matrix.postTranslate(mid.x - lastMid.x, mid.y - lastMid.y);
                        } else {
                            matrix.set(saved);
                            matrix.postTranslate(e.getX() - last.x, e.getY() - last.y);
                        }
                        setImageMatrix(matrix);
                        return true;
                    case MotionEvent.ACTION_POINTER_UP:
                        saved.set(matrix);
                        int remaining = e.getActionIndex() == 0 ? 1 : 0;
                        last.set(e.getX(remaining), e.getY(remaining));
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return false;
                }
            }
        }
    }
}
