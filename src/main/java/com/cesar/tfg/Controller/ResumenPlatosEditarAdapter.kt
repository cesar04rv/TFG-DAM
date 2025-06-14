package com.cesar.tfg.Controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Model.PlatoEditado
import com.cesar.tfg.R

// Adapter used to display the list of edited dishes in a command edit screen
class ResumenPlatosEditarAdapter(
    private val platos: List<PlatoEditado>,                      // List of edited dishes
    private val onEliminarClick: (PlatoEditado) -> Unit          // Callback for delete button
) : RecyclerView.Adapter<ResumenPlatosEditarAdapter.ViewHolder>() {

    // ViewHolder that holds the references to the UI components of each item
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.tvNombrePlato)         // Dish name
        val info: TextView = itemView.findViewById(R.id.tvCantidadPrecio)        // Quantity and price
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarPlato)   // Delete button
    }

    // Inflate the layout for a dish item and return a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plato_comanda, parent, false)
        return ViewHolder(view)
    }

    // Bind the dish data to the ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = platos[position]
        val total = item.cantidadOriginal
        val ronda = item.cantidadRonda

        holder.nombre.text = item.plato.nombre

        // Show round info only if there's an extra amount added
        holder.info.text = if (ronda > 0) {
            "x$total   ${item.plato.precio}€   x$ronda"
        } else {
            "x$total   ${item.plato.precio}€"
        }

        // Set delete button action
        holder.btnEliminar.setOnClickListener {
            onEliminarClick(item)
        }
    }

    // Return total number of items in the list
    override fun getItemCount(): Int = platos.size
}
