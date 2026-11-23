package com.buddyapps.apphub;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Repositorio desde el que AppHub se actualizará a sí misma. */
public class SelfRepoActivity extends Activity {
    private EditText field;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StorePrefs.init(this);
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, "Actualizaciones de AppHub", "Repositorio de GitHub",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { finish(); }
                }, null));
        root.addView(Ui.divider(this));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = Ui.vertical(this);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 24));
        scroll.addView(box, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout card = Ui.card(this);
        card.addView(Ui.tv(this, "¿Dónde se publica AppHub?", 16, Ui.TEXT, true));
        TextView info = Ui.tv(this,
                "Cuando subas una versión nueva a los Releases de GitHub, escribe aquí owner/repo para que AppHub pueda detectarla e instalarla.",
                13, Ui.MUTED, false);
        Ui.margins(info, 0, 6, 0, 0, this);
        card.addView(info);

        field = new EditText(this);
        field.setHint("owner/repo");
        field.setSingleLine(true);
        field.setText(StorePrefs.selfRepo() == null ? "" : StorePrefs.selfRepo());
        field.setTextColor(Ui.TEXT);
        field.setHintTextColor(Ui.MUTED);
        GradientDrawable bg = Ui.rounded(Ui.BG, 10);
        bg.setStroke(Ui.dp(this, 1), Ui.LINE);
        field.setBackground(bg);
        field.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.topMargin = Ui.dp(this, 14);
        field.setLayoutParams(fp);
        card.addView(field);

        TextView save = Ui.primaryButton(this, "Guardar repositorio");
        Ui.margins(save, 0, 14, 0, 0, this);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        card.addView(save);

        TextView clear = Ui.tv(this, "Quitar repositorio", 13, Ui.RED, false);
        Ui.margins(clear, 0, 12, 0, 0, this);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                StorePrefs.setSelfRepo("");
                Toast.makeText(SelfRepoActivity.this, "Repositorio quitado", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        card.addView(clear);
        box.addView(card);
        setContentView(root);
    }

    private void save() {
        String value = field.getText().toString().trim();
        String[] parts = value.split("/");
        if (parts.length != 2 || parts[0].length() == 0 || parts[1].length() == 0) {
            Toast.makeText(this, "Usa el formato owner/repo", Toast.LENGTH_SHORT).show();
            return;
        }
        StorePrefs.setSelfRepo(value);
        Toast.makeText(this, "Repositorio guardado", Toast.LENGTH_SHORT).show();
        finish();
    }
}
