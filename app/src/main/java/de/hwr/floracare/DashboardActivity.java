package de.hwr.floracare;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Dashboard.
 *
 * Stand Etappe 2: Begrüßung + Anzeige der Familie (Name + Einladungscode).
 * Der Code wird hier dauerhaft sichtbar, damit Familienmitglieder ihn jederzeit
 * zum Teilen wiederfinden. In Etappe 5 wird hieraus die "Heute fällig"-Übersicht.
 *
 * Prüfungs-Wissen: Hier passieren ZWEI VERKETTETE Firestore-Reads. Erst das
 * Nutzer-Dokument (liefert Name + familyId), und ERST WENN die familyId da ist,
 * das zugehörige Family-Dokument (liefert Familienname + Code). Der zweite Read
 * hängt vom Ergebnis des ersten ab -- deshalb steht er in dessen Callback.
 */
public class DashboardActivity extends AppCompatActivity {

    private TextView textWillkommen;
    private TextView textFamilie;
    private TextView textCode;
    private Button buttonLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textWillkommen = findViewById(R.id.text_willkommen);
        textFamilie = findViewById(R.id.text_familie);
        textCode = findViewById(R.id.text_code);
        buttonLogout = findViewById(R.id.button_logout);

        datenLaden();

        buttonLogout.setOnClickListener(v -> logout());
    }

    /** Schritt 1: Nutzer-Dokument lesen (Name + familyId). */
    private void datenLaden() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(nutzerDok -> {
                    String name = nutzerDok.getString("displayName");
                    textWillkommen.setText(getString(R.string.willkommen, name));

                    String familyId = nutzerDok.getString("familyId");
                    if (familyId != null && !familyId.isEmpty()) {
                        familieLaden(familyId);   // Schritt 2 nur, wenn Familie vorhanden
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.fehler_laden) + " " + e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /** Schritt 2: zugehöriges Family-Dokument lesen (Name + Einladungscode). */
    private void familieLaden(String familyId) {
        db.collection("families").document(familyId).get()
                .addOnSuccessListener(familyDok -> {
                    String familyName = familyDok.getString("name");
                    String code = familyDok.getString("inviteCode");
                    textFamilie.setText(getString(R.string.dashboard_familie, familyName));
                    textCode.setText(getString(R.string.dashboard_code, code));
                });
    }

    /**
     * Abmelden und zurück zum Login.
     * NEW_TASK | CLEAR_TASK leeren den Activity-Stack -- nach dem Logout kommt man
     * mit der Zurück-Taste nicht wieder in die geschützte App.
     */
    private void logout() {
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
