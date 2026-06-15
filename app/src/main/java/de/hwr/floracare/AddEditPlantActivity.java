package de.hwr.floracare;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Pflanze ANLEGEN oder BEARBEITEN – ein Screen, zwei Modi (+ Löschen).
 *
 * Der Modus wird über das Intent-Extra "plantId" gesteuert:
 *   - plantId == null  -> ANLEGEN  (leeres Formular, kein Löschen-Button)
 *   - plantId != null  -> BEARBEITEN (Formular vorgefüllt, Löschen sichtbar)
 *
 * Prüfungs-Wissen: Ein Screen für beide Fälle vermeidet doppelten Code. Die
 * Fallunterscheidung passiert an genau einer Stelle (plantId vorhanden?).
 * Beim Anlegen nutzen wir add() (neues Dokument mit automatischer ID), beim
 * Bearbeiten set() auf document(plantId) (genau dieses Dokument überschreiben).
 */
public class AddEditPlantActivity extends AppCompatActivity {

    private TextView textTitel;
    private EditText inputName, inputArt, inputBesitzer, inputGiessen, inputDuengen;
    private Button buttonSpeichern, buttonLoeschen;
    private ProgressBar ladeAnzeige;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String plantId = null;        // null => Anlege-Modus
    private String familyId = null;       // für neue Pflanzen nötig
    private Plant geladenePflanze = null; // im Bearbeiten-Modus die Originaldaten

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_plant);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textTitel = findViewById(R.id.text_titel);
        inputName = findViewById(R.id.input_name);
        inputArt = findViewById(R.id.input_art);
        inputBesitzer = findViewById(R.id.input_besitzer);
        inputGiessen = findViewById(R.id.input_giessen);
        inputDuengen = findViewById(R.id.input_duengen);
        buttonSpeichern = findViewById(R.id.button_speichern);
        buttonLoeschen = findViewById(R.id.button_loeschen);
        ladeAnzeige = findViewById(R.id.lade_anzeige);

        // Modus anhand des übergebenen Extras bestimmen
        plantId = getIntent().getStringExtra("plantId");

        if (plantId == null) {
            // ----- Anlege-Modus -----
            textTitel.setText(getString(R.string.add_plant_titel));
            buttonLoeschen.setVisibility(View.GONE);
            familyIdLaden();   // brauchen wir, um die Pflanze der Familie zuzuordnen
        } else {
            // ----- Bearbeiten-Modus -----
            textTitel.setText(getString(R.string.edit_plant_titel));
            buttonLoeschen.setVisibility(View.VISIBLE);
            pflanzeLaden();
        }

        buttonSpeichern.setOnClickListener(v -> speichern());
        buttonLoeschen.setOnClickListener(v -> loeschenBestaetigen());
    }

    /** Anlege-Modus: familyId des aktuellen Nutzers holen. */
    private void familyIdLaden() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(dok -> familyId = dok.getString("familyId"))
                .addOnFailureListener(e -> zeigeFehler(R.string.fehler_plant_laden, e));
    }

    /** Bearbeiten-Modus: bestehende Pflanze laden und Formular füllen. */
    private void pflanzeLaden() {
        db.collection("plants").document(plantId).get()
                .addOnSuccessListener(dok -> {
                    geladenePflanze = dok.toObject(Plant.class);
                    if (geladenePflanze == null) return;
                    familyId = geladenePflanze.getFamilyId();
                    inputName.setText(geladenePflanze.getName());
                    inputArt.setText(geladenePflanze.getSpecies());
                    inputBesitzer.setText(geladenePflanze.getOwnerLabel());
                    inputGiessen.setText(String.valueOf(geladenePflanze.getWaterIntervalDays()));
                    inputDuengen.setText(String.valueOf(geladenePflanze.getFertilizeIntervalDays()));
                })
                .addOnFailureListener(e -> zeigeFehler(R.string.fehler_plant_laden, e));
    }

    /** Validieren und speichern (legt an oder aktualisiert – je nach Modus). */
    private void speichern() {
        String name = inputName.getText().toString().trim();
        String art = inputArt.getText().toString().trim();           // optional
        String besitzer = inputBesitzer.getText().toString().trim();
        String giessenStr = inputGiessen.getText().toString().trim();
        String duengenStr = inputDuengen.getText().toString().trim();

        // ----- Validierung -----
        if (TextUtils.isEmpty(name)) {
            inputName.setError(getString(R.string.fehler_plant_name_leer));
            return;
        }
        if (TextUtils.isEmpty(besitzer)) {
            inputBesitzer.setError(getString(R.string.fehler_plant_owner_leer));
            return;
        }
        if (TextUtils.isEmpty(giessenStr)) {
            inputGiessen.setError(getString(R.string.fehler_plant_water_leer));
            return;
        }
        if (TextUtils.isEmpty(duengenStr)) {
            inputDuengen.setError(getString(R.string.fehler_plant_fertilize_leer));
            return;
        }
        int giessen = Integer.parseInt(giessenStr);   // sicher: Eingabefeld ist inputType=number
        int duengen = Integer.parseInt(duengenStr);
        if (giessen < 1) {
            inputGiessen.setError(getString(R.string.fehler_plant_intervall_null));
            return;
        }
        if (duengen < 1) {
            inputDuengen.setError(getString(R.string.fehler_plant_intervall_null));
            return;
        }
        if (familyId == null) {
            // Sicherheitsnetz: ohne familyId können wir die Pflanze nicht zuordnen
            Toast.makeText(this, getString(R.string.fehler_plant_speichern), Toast.LENGTH_LONG).show();
            return;
        }

        ladeStart();

        if (plantId == null) {
            // ----- Anlegen: neues Dokument mit automatischer ID -----
            String uid = auth.getCurrentUser().getUid();
            Plant neu = new Plant(familyId, name, art, besitzer, giessen, duengen, uid);
            db.collection("plants").add(neu)
                    .addOnSuccessListener(ref -> fertig(R.string.erfolg_plant_gespeichert))
                    .addOnFailureListener(e -> zeigeFehlerUndEntsperren(R.string.fehler_plant_speichern, e));
        } else {
            // ----- Aktualisieren: Originaldaten behalten, Formularfelder überschreiben -----
            // createdAt, createdBy, familyId bleiben erhalten (stehen im geladenen Objekt).
            geladenePflanze.setName(name);
            geladenePflanze.setSpecies(art);
            geladenePflanze.setOwnerLabel(besitzer);
            geladenePflanze.setWaterIntervalDays(giessen);
            geladenePflanze.setFertilizeIntervalDays(duengen);
            db.collection("plants").document(plantId).set(geladenePflanze)
                    .addOnSuccessListener(unused -> fertig(R.string.erfolg_plant_gespeichert))
                    .addOnFailureListener(e -> zeigeFehlerUndEntsperren(R.string.fehler_plant_speichern, e));
        }
    }

    private void loeschenBestaetigen() {
        String name = inputName.getText().toString().trim();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_titel))
                .setMessage(getString(R.string.dialog_delete_text, name))
                .setPositiveButton(getString(R.string.dialog_delete_ja), (d, w) -> loeschen())
                .setNegativeButton(getString(R.string.dialog_delete_nein), null)
                .show();
    }

    private void loeschen() {
        ladeStart();
        db.collection("plants").document(plantId).delete()
                .addOnSuccessListener(unused -> fertig(R.string.erfolg_plant_geloescht))
                .addOnFailureListener(e -> zeigeFehlerUndEntsperren(R.string.fehler_plant_loeschen, e));
    }

    private void fertig(int meldungResId) {
        ladeEnde();
        Toast.makeText(this, getString(meldungResId), Toast.LENGTH_SHORT).show();
        finish();   // zurück zur Liste; deren onResume() lädt automatisch neu
    }

    // ---------- kleine Helfer ----------
    private void zeigeFehler(int prefixResId, Exception e) {
        Toast.makeText(this, getString(prefixResId) + " " + e.getLocalizedMessage(),
                Toast.LENGTH_LONG).show();
    }

    private void zeigeFehlerUndEntsperren(int prefixResId, Exception e) {
        ladeEnde();
        zeigeFehler(prefixResId, e);
    }

    private void ladeStart() {
        ladeAnzeige.setVisibility(View.VISIBLE);
        buttonSpeichern.setEnabled(false);
        buttonLoeschen.setEnabled(false);
    }

    private void ladeEnde() {
        ladeAnzeige.setVisibility(View.GONE);
        buttonSpeichern.setEnabled(true);
        buttonLoeschen.setEnabled(true);
    }
}
