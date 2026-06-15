package de.hwr.floracare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zeigt alle Pflanzen der eigenen Familie als Liste (RecyclerView).
 *  - Button "Pflanze hinzufügen" -> AddEditPlantActivity (Anlege-Modus)
 *  - Tippen auf eine Pflanze      -> AddEditPlantActivity (Bearbeiten-Modus)
 *
 * Geladen wird in onResume(), damit die Liste nach Anlegen/Bearbeiten/Löschen
 * automatisch aktuell ist, sobald man hierher zurückkehrt.
 */
public class PlantListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textLeer;
    private Button buttonHinzufuegen;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Plant> pflanzen = new ArrayList<>();
    private PlantAdapter adapter;

    private String familyId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recycler_plants);
        textLeer = findViewById(R.id.text_leer);
        buttonHinzufuegen = findViewById(R.id.button_pflanze_hinzufuegen);

        // RecyclerView einrichten: LayoutManager (vertikale Liste) + Adapter + Trennlinien
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new PlantAdapter(pflanzen, this::pflanzeGeklickt);
        recyclerView.setAdapter(adapter);

        buttonHinzufuegen.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditPlantActivity.class)));

        familyIdLaden();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Beim Zurückkehren neu laden (familyId ist dann bereits bekannt).
        if (familyId != null) {
            pflanzenLaden();
        }
    }

    /** familyId aus dem Nutzer-Dokument holen, danach erstmals Pflanzen laden. */
    private void familyIdLaden() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(dok -> {
                    familyId = dok.getString("familyId");
                    pflanzenLaden();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.fehler_plant_laden) + " " + e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /** Alle Pflanzen laden, deren familyId zu unserer Familie passt. */
    private void pflanzenLaden() {
        db.collection("plants")
                .whereEqualTo("familyId", familyId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    pflanzen.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Plant p = doc.toObject(Plant.class);
                        if (p != null) pflanzen.add(p);
                    }
                    // Clientseitig nach Name sortieren – spart einen Firestore-Index,
                    // den ein orderBy zusammen mit dem whereEqualTo sonst bräuchte.
                    Collections.sort(pflanzen, (a, b) ->
                            a.getName().compareToIgnoreCase(b.getName()));
                    adapter.notifyDataSetChanged();
                    leeransichtAktualisieren();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.fehler_plant_laden) + " " + e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /** "Noch keine Pflanzen"-Text zeigen, wenn die Liste leer ist. */
    private void leeransichtAktualisieren() {
        boolean leer = pflanzen.isEmpty();
        textLeer.setVisibility(leer ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(leer ? View.GONE : View.VISIBLE);
    }

    /** Tippen auf eine Pflanze -> Bearbeiten-Modus (plantId mitgeben). */
    private void pflanzeGeklickt(Plant plant) {
        Intent intent = new Intent(this, AddEditPlantActivity.class);
        intent.putExtra("plantId", plant.getId());
        startActivity(intent);
    }
}
