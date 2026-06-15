package de.hwr.floracare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter für die Pflanzenliste (RecyclerView).
 *
 * Prüfungs-Wissen: Ein RecyclerView zeigt lange Listen effizient, indem er nur so
 * viele Item-Views erzeugt, wie auf den Bildschirm passen, und sie beim Scrollen
 * WIEDERVERWENDET ("recycling") – statt für 1000 Einträge 1000 Views zu bauen.
 * Der Adapter ist die Brücke zwischen Daten (List<Plant>) und den Views:
 *   - onCreateViewHolder: erzeugt eine neue, leere Item-View
 *   - onBindViewHolder:   befüllt eine (oft wiederverwendete) Item-View mit den
 *                         Daten an Position i
 *   - getItemCount:       Anzahl der Einträge
 * Der ViewHolder hält die gefundenen Views einer Zeile fest, damit findViewById
 * nicht bei jedem Scrollschritt erneut laufen muss.
 */
public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    /** Callback, damit die Activity auf Klicks reagieren kann. */
    public interface OnPlantClickListener {
        void onPlantClick(Plant plant);
    }

    private final List<Plant> pflanzen;
    private final OnPlantClickListener clickListener;

    public PlantAdapter(List<Plant> pflanzen, OnPlantClickListener clickListener) {
        this.pflanzen = pflanzen;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plant, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = pflanzen.get(position);

        holder.name.setText(plant.getName());

        // Art ist optional -> Zeile ausblenden, wenn leer
        if (plant.getSpecies() == null || plant.getSpecies().trim().isEmpty()) {
            holder.species.setVisibility(View.GONE);
        } else {
            holder.species.setVisibility(View.VISIBLE);
            holder.species.setText(plant.getSpecies());
        }

        holder.care.setText(holder.itemView.getContext().getString(
                R.string.item_care_info, plant.getOwnerLabel(), plant.getWaterIntervalDays()));

        // Klick auf die ganze Zeile -> Activity informieren
        holder.itemView.setOnClickListener(v -> clickListener.onPlantClick(plant));
    }

    @Override
    public int getItemCount() {
        return pflanzen.size();
    }

    /** Hält die Views einer einzelnen Listenzeile. */
    static class PlantViewHolder extends RecyclerView.ViewHolder {
        TextView name, species, care;

        PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_name);
            species = itemView.findViewById(R.id.item_species);
            care = itemView.findViewById(R.id.item_care);
        }
    }
}
