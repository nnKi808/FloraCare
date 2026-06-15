package de.hwr.floracare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Einstiegspunkt und ZENTRALER ROUTER der App (Launcher-Activity).
 *
 * Ab Etappe 2 entscheidet diese Activity über DREI mögliche Ziele:
 *   1. nicht eingeloggt            -> LoginActivity
 *   2. eingeloggt, ABER ohne Familie -> FamilySetupActivity
 *   3. eingeloggt UND mit Familie    -> DashboardActivity
 *
 * Zeigt selbst keine Oberfläche -- prüft nur und leitet weiter.
 *
 * Prüfungs-Wissen: Die Routing-Logik liegt BEWUSST nur an dieser einen Stelle.
 * Andere Screens (z. B. LoginActivity) schicken nach erfolgreichem Login einfach
 * wieder hierher zurück -- so gibt es eine einzige Quelle der Wahrheit für die
 * Frage "wohin gehört dieser Nutzer gerade?". Der familyId-Check ist ein
 * asynchroner Firestore-Read, deshalb passiert die Weiterleitung im Callback.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kein setContentView: reiner Router ohne eigene Oberfläche.

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser nutzer = auth.getCurrentUser();

        // Fall 1: Niemand eingeloggt -> Login
        if (nutzer == null) {
            weiterZu(LoginActivity.class);
            return;
        }

        // Fall 2/3: Eingeloggt -> familyId aus Firestore lesen und dann entscheiden
        db.collection("users").document(nutzer.getUid()).get()
                .addOnSuccessListener(dokument -> {
                    String familyId = dokument.getString("familyId");
                    if (familyId == null || familyId.isEmpty()) {
                        // Eingeloggt, aber noch keiner Familie zugeordnet
                        weiterZu(FamilySetupActivity.class);
                    } else {
                        // Vollständig eingerichtet
                        weiterZu(DashboardActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    // familyId-Status nicht lesbar (z. B. offline ohne Cache).
                    // Sichere Rückfalloption: zurück zum Login, dort kann neu probiert werden.
                    Toast.makeText(this, getString(R.string.fehler_route),
                            Toast.LENGTH_LONG).show();
                    weiterZu(LoginActivity.class);
                });
    }

    /** Startet das Ziel und entfernt den Router vom Stack (kein Zurück hierher). */
    private void weiterZu(Class<?> ziel) {
        startActivity(new Intent(this, ziel));
        finish();
    }
}
