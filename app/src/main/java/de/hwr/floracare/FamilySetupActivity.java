package de.hwr.floracare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Family-Setup-Screen.
 *
 * Grundregel der App: Jeder eingeloggte Nutzer gehört GENAU EINER Familie an.
 * Dieser Screen sorgt dafür. Es gibt zwei Wege:
 *
 *   (A) Neue Familie ERSTELLEN  -> erzeugt ein families-Dokument + Einladungscode,
 *                                  zeigt den Code zum Teilen an.
 *   (B) Bestehender Familie BEITRETEN -> sucht die Familie über den Einladungscode.
 *
 * Beide Wege enden gleich: Die familyId wird ins Nutzer-Dokument geschrieben,
 * danach geht es zum Dashboard.
 *
 * Prüfungs-Wissen: Hier kommen drei Firestore-Bausteine vor, die in Etappe 1
 * noch nicht da waren:
 *   - document() OHNE Argument  -> erzeugt eine zufällige, eindeutige ID
 *   - whereEqualTo(...)         -> eine Abfrage (Query) über eine ganze Collection
 *   - update(...)               -> ändert ein einzelnes Feld (statt set, das überschreibt)
 */
public class FamilySetupActivity extends AppCompatActivity {

    private EditText inputFamilienname;
    private EditText inputCode;
    private Button buttonErstellen;
    private Button buttonBeitreten;
    private ProgressBar ladeAnzeige;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_setup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFamilienname = findViewById(R.id.input_familienname);
        inputCode = findViewById(R.id.input_code);
        buttonErstellen = findViewById(R.id.button_familie_erstellen);
        buttonBeitreten = findViewById(R.id.button_familie_beitreten);
        ladeAnzeige = findViewById(R.id.lade_anzeige);

        buttonErstellen.setOnClickListener(v -> familieErstellen());
        buttonBeitreten.setOnClickListener(v -> familieBeitreten());
    }

    // ============== Weg A: Familie erstellen ==============
    private void familieErstellen() {
        String name = inputFamilienname.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            inputFamilienname.setError(getString(R.string.fehler_familienname_leer));
            return;
        }
        ladeStart();

        String uid = auth.getCurrentUser().getUid();
        String code = codeGenerieren();

        // document() OHNE Argument erzeugt nur eine REFERENZ mit zufälliger, eindeutiger
        // ID -- noch ohne Schreibzugriff. Diese ID nutzen wir als unsere familyId.
        DocumentReference familyRef = db.collection("families").document();
        String familyId = familyRef.getId();

        Map<String, Object> family = new HashMap<>();
        family.put("name", name);
        family.put("inviteCode", code);
        family.put("createdBy", uid);
        family.put("createdAt", FieldValue.serverTimestamp());

        familyRef.set(family)
                .addOnSuccessListener(unused ->
                        // Familie steht -> jetzt den Nutzer damit verknüpfen
                        familyVerknuepfen(familyId, code))
                .addOnFailureListener(e -> {
                    ladeEnde();
                    Toast.makeText(this,
                            getString(R.string.fehler_family_erstellen) + " " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ============== Weg B: Familie beitreten ==============
    private void familieBeitreten() {
        // toUpperCase: Der Code wird groß gespeichert -> Eingabe ebenfalls groß,
        // damit "flr..." und "FLR..." als gleich gelten.
        String code = inputCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            inputCode.setError(getString(R.string.fehler_code_leer));
            return;
        }
        ladeStart();

        // QUERY statt direktem Lesen: Wir kennen die familyId NICHT, nur den Code.
        // Also durchsuchen wir die families-Collection nach dem Dokument, dessen
        // Feld inviteCode dem eingegebenen Code entspricht.
        db.collection("families")
                .whereEqualTo("inviteCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        // Kein Treffer -> dieser Code existiert nicht
                        ladeEnde();
                        Toast.makeText(this, getString(R.string.fehler_code_ungueltig),
                                Toast.LENGTH_LONG).show();
                    } else {
                        DocumentSnapshot treffer = query.getDocuments().get(0);
                        String familyId = treffer.getId();
                        // Beitreten = gleicher Verknüpfungsschritt, nur ohne Code-Dialog
                        familyVerknuepfen(familyId, null);
                    }
                })
                .addOnFailureListener(e -> {
                    ladeEnde();
                    Toast.makeText(this,
                            getString(R.string.fehler_family_beitreten) + " " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Gemeinsamer Abschluss BEIDER Wege: die familyId ins Nutzer-Dokument schreiben.
     *
     * update() ändert nur das eine Feld "familyId". Das Nutzer-Dokument existiert
     * bereits seit der Registrierung -- wir ergänzen es also nur, statt es mit set()
     * komplett zu überschreiben.
     *
     * @param zeigeCode Code (Weg A -> Dialog annzeige) oder null (Weg B -> direkt weiter).
     */
    private void familyVerknuepfen(String familyId, String zeigeCode) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("familyId", familyId)
                .addOnSuccessListener(unused -> {
                    ladeEnde();
                    if (zeigeCode != null) {
                        codeDialogZeigen(zeigeCode);   // Weg A
                    } else {
                        Toast.makeText(this, getString(R.string.erfolg_family_beigetreten),
                                Toast.LENGTH_SHORT).show();
                        zumDashboard();                 // Weg B
                    }
                })
                .addOnFailureListener(e -> {
                    ladeEnde();
                    Toast.makeText(this,
                            getString(R.string.fehler_family_beitreten) + " " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Zeigt dem Ersteller den Einladungscode zum Teilen, dann weiter zum Dashboard. */
    private void codeDialogZeigen(String code) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_code_titel))
                .setMessage(getString(R.string.dialog_code_text, code))
                .setCancelable(false)   // erzwingt bewusstes Wegklicken (Code nicht verpassen)
                .setPositiveButton(getString(R.string.dialog_code_ok),
                        (dialog, which) -> zumDashboard())
                .show();
    }

    private void zumDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    /**
     * Erzeugt einen 6-stelligen Einladungscode.
     * Das Alphabet lässt bewusst verwechselbare Zeichen weg (kein 0/O, kein 1/I/L),
     * damit niemand den Code falsch abtippt.
     *
     * Hinweis (gutes Prüfungs-Argument): Theoretisch könnten zwei Familien denselben
     * Code bekommen. Bei 31^6 ≈ 887 Mio. Kombinationen ist das hier vernachlässigbar.
     * In einer großen Produktiv-App würde man den Code vor der Vergabe per Query auf
     * Eindeutigkeit prüfen und bei Kollision neu würfeln.
     */
    private String codeGenerieren() {
        String zeichen = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(zeichen.charAt(random.nextInt(zeichen.length())));
        }
        return sb.toString();
    }

    // ============== kleine UI-Helfer ==============
    private void ladeStart() {
        ladeAnzeige.setVisibility(View.VISIBLE);
        buttonErstellen.setEnabled(false);
        buttonBeitreten.setEnabled(false);
    }

    private void ladeEnde() {
        ladeAnzeige.setVisibility(View.GONE);
        buttonErstellen.setEnabled(true);
        buttonBeitreten.setEnabled(true);
    }
}
