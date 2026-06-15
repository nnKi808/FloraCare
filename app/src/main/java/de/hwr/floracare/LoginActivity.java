package de.hwr.floracare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Login-Screen.
 *
 * Ablauf:
 * 1. Nutzer gibt E-Mail + Passwort ein und drückt "Anmelden".
 * 2. Eingaben werden lokal validiert (nicht leer).
 * 3. FirebaseAuth prüft die Anmeldedaten SERVERSEITIG
 *    (signInWithEmailAndPassword — asynchroner Netzwerk-Call).
 * 4. Erfolg  -> weiter zum Dashboard.
 *    Fehler  -> Fehlermeldung als Toast.
 *
 * Prüfungs-Wissen: Der Firebase-Call ist asynchron. Das Ergebnis kommt
 * über einen Callback (OnCompleteListener) zurück — die App friert
 * währenddessen nicht ein. Deshalb der Lade-Spinner.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPasswort;
    private Button buttonLogin;
    private TextView linkRegistrieren;
    private ProgressBar ladeAnzeige;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase-Auth-Instanz holen (Singleton, von Firebase verwaltet)
        auth = FirebaseAuth.getInstance();

        // UI-Elemente aus dem Layout mit dem Java-Code verbinden
        inputEmail = findViewById(R.id.input_email);
        inputPasswort = findViewById(R.id.input_passwort);
        buttonLogin = findViewById(R.id.button_login);
        linkRegistrieren = findViewById(R.id.link_registrieren);
        ladeAnzeige = findViewById(R.id.lade_anzeige);

        // Klick-Verhalten festlegen (Lambda-Ausdruck = kurze Schreibweise
        // für einen OnClickListener)
        buttonLogin.setOnClickListener(v -> loginVersuchen());

        linkRegistrieren.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    /**
     * Validiert die Eingaben und startet den Firebase-Login.
     */
    private void loginVersuchen() {
        String email = inputEmail.getText().toString().trim();
        String passwort = inputPasswort.getText().toString().trim();

        // --- Lokale Validierung (bevor wir das Netzwerk bemühen) ---
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError(getString(R.string.fehler_email_leer));
            return;
        }
        if (TextUtils.isEmpty(passwort)) {
            inputPasswort.setError(getString(R.string.fehler_passwort_leer));
            return;
        }

        // UI in den "Lade-Zustand" versetzen
        ladeAnzeige.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        // --- Serverseitige Prüfung durch Firebase ---
        auth.signInWithEmailAndPassword(email, passwort)
                .addOnCompleteListener(task -> {
                    // Dieser Code läuft erst, wenn die Antwort vom Server da ist.
                    ladeAnzeige.setVisibility(View.GONE);
                    buttonLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Login erfolgreich -> zum Dashboard, Login-Screen schließen
                        // Nach dem Login entscheidet der Router (MainActivity), wohin es geht:
                        // Family-Setup (noch keine Familie) oder Dashboard (Familie vorhanden).
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        // Login fehlgeschlagen -> Grund anzeigen
                        String grund = (task.getException() != null)
                                ? task.getException().getLocalizedMessage()
                                : getString(R.string.fehler_unbekannt);
                        Toast.makeText(this,
                                getString(R.string.fehler_login) + " " + grund,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
