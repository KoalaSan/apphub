package com.buddyapps.apphub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

/** Se abre al terminar una descarga para preguntar si se quiere instalar ya. */
public class InstallPromptActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String name = getIntent().getStringExtra("appName");
        final String uriText = getIntent().getStringExtra("uri");
        final long id = getIntent().getLongExtra("id", -1);
        final Uri uri = uriText == null ? null : Uri.parse(uriText);

        String title = "Descarga completada";
        if (name != null && name.length() > 0) title += " · " + name;
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("El APK está listo. ¿Quieres abrir el instalador ahora?")
                .setNegativeButton("Ahora no", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) { finish(); }
                });
        if (uri != null) {
            b.setPositiveButton("Instalar", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int which) {
                    if (InstallHelper.canInstall(InstallPromptActivity.this)) {
                        InstallHelper.install(InstallPromptActivity.this, uri);
                    } else {
                        InstallHelper.openInstallSettings(InstallPromptActivity.this);
                    }
                    finish();
                }
            });
        }
        b.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel(DialogInterface d) { finish(); }
        });
        b.show();
    }
}
