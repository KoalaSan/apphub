package com.buddyapps.apphub;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Utilidades de UI: una sola paleta y una sola métrica para toda la app. */
public final class Ui {
    public static final int PRIMARY = 0xFF2F6DF6;
    public static final int PRIMARY_DARK = 0xFF1E4FBF;
    public static final int BG = 0xFFF5F7FB;
    public static final int CARD = 0xFFFFFFFF;
    public static final int TEXT = 0xFF172033;
    public static final int MUTED = 0xFF61708A;
    public static final int LINE = 0xFFE3E9F2;
    public static final int GREEN = 0xFF16904A;
    public static final int GREEN_BG = 0xFFE8F7EE;
    public static final int AMBER = 0xFF9A6400;
    public static final int AMBER_BG = 0xFFFFF4DC;
    public static final int RED = 0xFFB42318;

    private Ui() { }

    public static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics()));
    }

    public static TextView tv(Context c, String text, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text == null ? "" : text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    public static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    public static GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(radiusDp);
        return d;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = vertical(c);
        l.setBackgroundColor(Color.TRANSPARENT);
        GradientDrawable d = rounded(CARD, 16);
        d.setStroke(dp(c, 1), LINE);
        l.setBackground(d);
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        l.setElevation(dp(c, 1));
        return l;
    }

    public static TextView button(Context c, String text, int bg, int fg) {
        TextView b = tv(c, text, 14, fg, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(bg, 12));
        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        b.setMinHeight(dp(c, 46));
        return b;
    }

    public static TextView primaryButton(Context c, String text) {
        return button(c, text, PRIMARY, 0xFFFFFFFF);
    }

    public static TextView secondaryButton(Context c, String text) {
        GradientDrawable d = rounded(CARD, 12);
        d.setStroke(dp(c, 1), PRIMARY);
        TextView b = button(c, text, CARD, PRIMARY);
        b.setBackground(d);
        return b;
    }

    public static ImageView icon(Context c, int res, int color, int sizeDp) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int s = dp(c, sizeDp);
        iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return iv;
    }

    public static void margins(View v, int left, int top, int right, int bottom, Context c) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            m.leftMargin = dp(c, left);
            m.topMargin = dp(c, top);
            m.rightMargin = dp(c, right);
            m.bottomMargin = dp(c, bottom);
        }
    }

    public static LinearLayout header(Context c, String title, String subtitle,
                                      View.OnClickListener back, View.OnClickListener refresh) {
        LinearLayout h = horizontal(c);
        h.setBackgroundColor(CARD);
        h.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
        if (back != null) {
            ImageView iv = icon(c, R.drawable.ic_arrow_back, TEXT, 24);
            iv.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 8));
            iv.setOnClickListener(back);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_VERTICAL;
            iv.setLayoutParams(lp);
            h.addView(iv);
        }
        LinearLayout texts = vertical(c);
        texts.addView(tv(c, title, 20, TEXT, true));
        if (subtitle != null) texts.addView(tv(c, subtitle, 12, MUTED, false));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMargins(dp(c, back == null ? 4 : 0), 0, dp(c, 8), 0);
        texts.setLayoutParams(tp);
        h.addView(texts);
        if (refresh != null) {
            ImageView iv = icon(c, R.drawable.ic_refresh, PRIMARY, 22);
            iv.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 8));
            iv.setOnClickListener(refresh);
            iv.setContentDescription("Actualizar");
            h.addView(iv);
        }
        return h;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }
}
