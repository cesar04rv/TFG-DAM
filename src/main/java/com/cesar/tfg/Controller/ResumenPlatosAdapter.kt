package com.cesar.tfg.Controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Model.Plato
import com.cesar.tfg.R

// Adapter used to display a summary of selected dishes (Platos) in a RecyclerView
class ResumenPlatosAdapter(
    private val platosList: List<Pair<Plato, Int>>, // List of pairs: (dish, quantity)
    private val onEliminarClick: (Plato) -> Unit     // Callback when delete button is clicked
) : RecyclerView.Adapter<ResumenPlatosAdapter.PlatosViewHolder>() {

    // ViewHolder that holds the UI elements for each dish item in the summary
    inner class PlatosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePlato)           // Dish name
        val tvCantidadPrecio: TextView = itemView.findViewById(R.id.tvCantidadPrecio) // Quantity and price
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarPlato)        // Delete button
    }

    // Inflate the layout for a single item and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatosViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plato_comanda, parent, false)
        return PlatosViewHolder(view)
    }

    // Bind dish data to the ViewHolder
    override fun onBindViewHolder(holder: PlatosViewHolder, position: Int) {
        val (plato, cantidad) = platosList[position]
        holder.tvNombre.text = plato.nombre
        holder.tvCantidadPrecio.text = "x$cantidad    ${plato.precio}€"

        // Handle delete button click
        holder.btnEliminar.setOnClickListener {
            onEliminarClick(plato)
        }
    }

    // Return the total number of items
    override fun getItemCount(): Int = platosList.size
}
