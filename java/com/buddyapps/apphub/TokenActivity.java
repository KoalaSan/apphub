package com.buddyapps.apphub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** El usuario pega aquí su token; Buddy nunca lo teclea ni lo envía a ningún servidor. */
public class TokenActivity extends Activity {
    private EditText field;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StorePrefs.init(this);
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        root.addView(Ui.header(this, "Conectar GitHub", "Para ver tus repositorios",
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
        card.addView(Ui.tv(this, "Token de acceso personal", 16, Ui.TEXT, true));
        TextView info = Ui.tv(this,
                "Se guarda solo en este dispositivo y se usa para consultar tu cuenta de GitHub. Nadie más lo recibe.",
                13, Ui.MUTED, false);
        Ui.margins(info, 0, 6, 0, 0, this);
        card.addView(info);

        field = new EditText(this);
        field.setHint("ghp_… o github_pat_…");
        field.setSingleLine(true);
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

        TextView save = Ui.primaryButton(this, "Guardar token");
        Ui.margins(save, 0, 14, 0, 0, this);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        card.addView(save);

        TextView create = Ui.tv(this, "Crear un token en GitHub →", 12, Ui.PRIMARY, false);
        Ui.margins(create, 0, 12, 0, 0, this);
        create.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/settings/tokens")));
            }
        });
        card.addView(create);
        box.addView(card);

        if (StorePrefs.hasToken()) {
            TextView remove = Ui.tv(this, "Quitar token guardado", 13, Ui.RED, false);
            Ui.margins(remove, 0, 18, 0, 0, this);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    StorePrefs.setToken("");
                    Toast.makeText(TokenActivity.this, "Token eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            box.addView(remove);
        }
        setContentView(root);
    }

    private void save() {
        String value = field.getText().toString().trim();
        if (value.length() < 10 || value.contains(" ")) {
            Toast.makeText(this, "Pega un token válido de GitHub", Toast.LENGTH_SHORT).show();
            return;
        }
        StorePrefs.setToken(value);
        Toast.makeText(this, "Token guardado en este dispositivo", Toast.LENGTH_SHORT).show();
        field.setText("");
        finish();
    }
}
