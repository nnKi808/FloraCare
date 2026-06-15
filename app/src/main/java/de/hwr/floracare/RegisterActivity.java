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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Registrierungs-Screen.
 *
 * Ablauf:
 * 1. Nutzer gibt Name, E-Mail, Passwort (2x) ein.
 * 2. Lokale Validierung (Felder gefüllt, Passwörter gleich, min. 6 Zeichen
 *    — das verlangt Firebase als Minimum).
 * 3. FirebaseAuth legt das Konto an (createUserWithEmailAndPassword).
 * 4. Zusätzlich legen wir ein Nutzer-Dokument in Firestore an
 *    (Collection "users"), denn FirebaseAuth speichert nur E-Mail/Passwort.
 *    Unseren Anzeigenamen + die spätere familyId verwalten WIR in Firestore.
 * 5. Erfolg -> weiter zum Dashboard.
 *
 * Prüfungs-Wissen: Zwei getrennte Systeme arbeiten hier zusammen:
 *   FirebaseAuth   = Identität (wer bist du? Passwort-Prüfung)
 *   Firestore      = Profildaten (wie heißt du? zu welcher Familie gehörst du?)
 * Verknüpft werden beide über die eindeutige User-ID (uid).
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText inputName;
    private EditText inputEmail;
    private EditText inputPasswort;
    private EditText inputPasswortWdh;
    private Button buttonRegistrieren;
    private TextView linkZumLogin;
    private ProgressBar ladeAnzeige;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPasswort = findViewById(R.id.input_passwort);
        inputPasswortWdh = findViewById(R.id.input_passwort_wdh);
        buttonRegistrieren = findViewById(R.id.button_registrieren);
        linkZumLogin = findViewById(R.id.link_login);
        ladeAnzeige = findViewById(R.id.lade_anzeige);

        buttonRegistrieren.setOnClickListener(v -> registrierungVersuchen());

        // Zurück zum Login (finish() statt neuem Intent:
        // LoginActivity liegt noch unter uns auf dem Activity-Stack)
        linkZumLogin.setOnClickListener(v -> finish());
    }

    private void registrierungVersuchen() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String passwort = inputPasswort.getText().toString().trim();
        String passwortWdh = inputPasswortWdh.getText().toString().trim();

        // --- Lokale Validierung ---
        if (TextUtils.isEmpty(name)) {
            inputName.setError(getString(R.string.fehler_name_leer));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError(getString(R.string.fehler_email_leer));
            return;
        }
        if (passwort.length() < 6) {
            // Firebase lehnt Passwörter unter 6 Zeichen ohnehin ab —
            // wir fangen das schon lokal ab (bessere Nutzerführung).
            inputPasswort.setError(getString(R.string.fehler_passwort_kurz));
            return;
        }
        if (!passwort.equals(passwortWdh)) {
            inputPasswortWdh.setError(getString(R.string.fehler_passwort_ungleich));
            return;
        }

        ladeAnzeige.setVisibility(View.VISIBLE);
        buttonRegistrieren.setEnabled(false);

        // --- Schritt 1: Konto bei FirebaseAuth anlegen ---
        auth.createUserWithEmailAndPassword(email, passwort)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // --- Schritt 2: Nutzer-Dokument in Firestore anlegen ---
                        nutzerDokumentAnlegen(name, email);
                    } else {
                        ladeAnzeige.setVisibility(View.GONE);
                        buttonRegistrieren.setEnabled(true);
                        String grund = (task.getException() != null)
                                ? task.getException().getLocalizedMessage()
                                : getString(R.string.fehler_unbekannt);
                        Toast.makeText(this,
                                getString(R.string.fehler_registrierung) + " " + grund,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Legt das Profil-Dokument unter users/{uid} an.
     * familyId bleibt vorerst null — sie wird in Etappe 2
     * (Family-Setup) gesetzt.
     */
    private void nutzerDokumentAnlegen(String name, String email) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> nutzerDaten = new HashMap<>();
        nutzerDaten.put("displayName", name);
        nutzerDaten.put("email", email);
        nutzerDaten.put("familyId", null);
        // serverTimestamp(): Der FIRESTORE-SERVER setzt die Uhrzeit —
        // nicht das Handy (dessen Uhr könnte falsch gehen).
        nutzerDaten.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(nutzerDaten)
                .addOnSuccessListener(unused -> {
                    ladeAnzeige.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.erfolg_registrierung),
                            Toast.LENGTH_SHORT).show();
                    // Frisch registrierte Nutzer haben noch KEINE Familie -> direkt zum Family-Setup.
                    startActivity(new Intent(this, FamilySetupActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    ladeAnzeige.setVisibility(View.GONE);
                    buttonRegistrieren.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.fehler_profil) + " " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
