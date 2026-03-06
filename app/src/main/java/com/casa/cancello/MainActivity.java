package com.casa.cancello;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String GATE_URL =
        "https://script.google.com/macros/s/AKfycbwGmke2bkgtV_eCcIiZzOUFuDp-INKFBKK8Lva1GiA8FbFFHdkpL5EEEU6kwQWE8_e_3A/exec?action=cancello";

    private Button btnCancello;
    private TextView tvStatus;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCancello = findViewById(R.id.btnCancello);
        tvStatus    = findViewById(R.id.tvStatus);

        btnCancello.setOnClickListener(v -> apriCancello());
    }

    private void apriCancello() {
        btnCancello.setEnabled(false);
        tvStatus.setText("Apertura in corso...");
        tvStatus.setTextColor(Color.parseColor("#FFA500"));

        new Thread(() -> {
            String result = null;
            Exception error = null;

            try {
                // Google Script redirects: follow manually to handle HTTPS->HTTPS redirects
                String currentUrl = GATE_URL;
                HttpURLConnection conn = null;

                for (int redirects = 0; redirects < 5; redirects++) {
                    URL url = new URL(currentUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(10_000);
                    conn.setInstanceFollowRedirects(false);

                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP
                            || code == HttpURLConnection.HTTP_MOVED_PERM
                            || code == 307 || code == 308) {
                        currentUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        continue;
                    }

                    // Read response body (discard content, just check success)
                    InputStream is = conn.getInputStream();
                    while (is.read() != -1) { /* drain */ }
                    is.close();
                    conn.disconnect();

                    if (code >= 200 && code < 300) {
                        result = "ok";
                    } else {
                        error = new Exception("HTTP " + code);
                    }
                    break;
                }
            } catch (Exception e) {
                error = e;
            }

            final String finalResult = result;
            final Exception finalError = error;

            mainHandler.post(() -> {
                btnCancello.setEnabled(true);
                if (finalResult != null) {
                    tvStatus.setText("Cancello aperto!");
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    String msg = finalError != null ? finalError.getMessage() : "Errore sconosciuto";
                    tvStatus.setText("Errore: " + msg);
                    tvStatus.setTextColor(Color.parseColor("#F44336"));
                }
            });
        }).start();
    }
}
