package com.buddyapps.apphub;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;

/** Animaciones ligeras (solo framework) para dar vida a la tienda. */
public final class Anim {
    private Anim() { }

    /** Entrada suave: aparece desde abajo con fade. */
    public static void slideIn(View v, int delayMs) {
        Animation a = new AnimationSetCompat();
        a.setStartOffset(delayMs);
        v.startAnimation(a);
    }

    /** Conjunto fade + subida, reutilizable. */
    private static final class AnimationSetCompat extends android.view.animation.AnimationSet {
        AnimationSetCompat() {
            super(true);
            AlphaAnimation fade = new AlphaAnimation(0f, 1f);
            TranslateAnimation up = new TranslateAnimation(0, 0, 40, 0);
            fade.setDuration(320);
            up.setDuration(320);
            DecelerateInterpolator d = new DecelerateInterpolator();
            fade.setInterpolator(d);
            up.setInterpolator(d);
            addAnimation(fade);
            addAnimation(up);
        }
    }

    public static void fadeIn(View v) {
        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(220);
        a.setInterpolator(new DecelerateInterpolator());
        v.startAnimation(a);
    }

    /** Pequeño rebote al aparecer, útil para contadores y resultados. */
    public static void pop(View v) {
        android.view.animation.ScaleAnimation s = new android.view.animation.ScaleAnimation(
                0.86f, 1f, 0.86f, 1f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        s.setDuration(360);
        s.setInterpolator(new OvershootInterpolator(1.2f));
        v.startAnimation(s);
    }

    /** Realimentación al pulsar: se encoge y vuelve a su tamaño. */
    public static void press(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
                } else if (e.getAction() == MotionEvent.ACTION_UP
                        || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                }
                return false; // no consumimos: el click sigue funcionando
            }
        });
    }

    /** Gira el icono de actualizar una vuelta completa. */
    public static void spin(View v) {
        RotateAnimation r = new RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        r.setDuration(700);
        r.setInterpolator(new DecelerateInterpolator());
        v.startAnimation(r);
    }

    /** Barra de progreso que avanza suavemente hasta el porcentaje dado. */
    public static void progress(final ProgressBar bar, int to) {
        int from = bar.getProgress();
        if (to <= from) { bar.setProgress(to); return; }
        ValueAnimator a = ValueAnimator.ofInt(from, to);
        a.setDuration(420);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator anim) {
                bar.setProgress((Integer) anim.getAnimatedValue());
            }
        });
        a.start();
    }
}
