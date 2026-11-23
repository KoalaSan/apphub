package com.buddyapps.apphub;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Estado de carga sencillo reutilizable en pantallas pequeñas. */
final class LinearLoading {
    private LinearLoading() { }

    static TextView show(LinearLayout container, Context c, String message) {
        TextView t = Ui.tv(c, message, 13, Ui.MUTED, false);
        t.setPadding(Ui.dp(c, 16), Ui.dp(c, 24), Ui.dp(c, 16), Ui.dp(c, 8));
        container.addView(t);
        return t;
    }
}
