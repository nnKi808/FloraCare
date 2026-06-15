package de.hwr.floracare;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Datenmodell für eine Pflanze (POJO – "Plain Old Java Object").
 *
 * Ein POJO ist eine schlichte Klasse aus Feldern + Gettern/Settern, ohne
 * Android- oder Firebase-Logik. Firestore wandelt Dokumente automatisch in
 * solche Objekte um (doc.toObject(Plant.class)) und Objekte zurück in Dokumente.
 *
 * Prüfungs-Wissen:
 * - Der LEERE Konstruktor ist PFLICHT: Firestore erzeugt das Objekt zuerst leer
 *   und füllt danach die Felder über die Setter. Fehlt er, kracht es beim Einlesen.
 * - @DocumentId: Firestore füllt dieses Feld automatisch mit der Dokument-ID.
 *   Die ID steht NICHT als normales Feld im Dokument; beim Schreiben wird sie
 *   ignoriert. So trägt jedes Plant-Objekt "weiß", woher es stammt.
 * - @ServerTimestamp: Ist createdAt beim Schreiben null, trägt der SERVER die
 *   aktuelle Zeit ein – nicht die evtl. falsch gehende Geräte-Uhr.
 *
 * (Die Pflege-Zeitstempel lastWatered/lastFertilized kommen in Etappe 4 dazu,
 *  wenn das Gießen/Düngen umgesetzt wird – dieses Modell wächst also noch.)
 */
public class Plant {

    @DocumentId
    private String id;

    private String familyId;
    private String name;
    private String species;
    private String ownerLabel;
    private int waterIntervalDays;
    private int fertilizeIntervalDays;
    private String createdBy;

    @ServerTimestamp
    private Timestamp createdAt;

    /** Pflicht-Konstruktor für Firestore (nicht entfernen!). */
    public Plant() {
    }

    /** Bequemer Konstruktor zum Anlegen einer neuen Pflanze. */
    public Plant(String familyId, String name, String species, String ownerLabel,
                 int waterIntervalDays, int fertilizeIntervalDays, String createdBy) {
        this.familyId = familyId;
        this.name = name;
        this.species = species;
        this.ownerLabel = ownerLabel;
        this.waterIntervalDays = waterIntervalDays;
        this.fertilizeIntervalDays = fertilizeIntervalDays;
        this.createdBy = createdBy;
        // id wird von Firestore vergeben, createdAt vom Server gesetzt
    }

    // ---------- Getter & Setter (von Firestore benötigt) ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getOwnerLabel() { return ownerLabel; }
    public void setOwnerLabel(String ownerLabel) { this.ownerLabel = ownerLabel; }

    public int getWaterIntervalDays() { return waterIntervalDays; }
    public void setWaterIntervalDays(int waterIntervalDays) { this.waterIntervalDays = waterIntervalDays; }

    public int getFertilizeIntervalDays() { return fertilizeIntervalDays; }
    public void setFertilizeIntervalDays(int fertilizeIntervalDays) { this.fertilizeIntervalDays = fertilizeIntervalDays; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
