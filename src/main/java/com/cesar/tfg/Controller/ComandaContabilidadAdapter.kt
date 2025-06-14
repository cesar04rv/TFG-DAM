package com.cesar.tfg.Controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Model.Comanda
import com.cesar.tfg.R
import java.text.SimpleDateFormat
import java.util.*

// Adapter used to display a list of finalized comandas in the accounting section
class ComandaContabilidadAdapter(
    private val comandas: List<Comanda>,
    private val onClick: (Comanda) -> Unit // Callback when a comanda item is clicked
) : RecyclerView.Adapter<ComandaContabilidadAdapter.ViewHolder>() {

    // ViewHolder that holds the UI elements for a single comanda item
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fecha: TextView = view.findViewById(R.id.tvFechaComanda) // Date and time
        val mesa: TextView = view.findViewById(R.id.tvMesaComanda)   // Table number
        val precio: TextView = view.findViewById(R.id.tvPrecioComanda) // Total price
    }

    // Inflate the item layout and create the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comanda_contabilidad, parent, false)
        return ViewHolder(v)
    }

    // Bind data from a Comanda object to the ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comanda = comandas[position]
        holder.fecha.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(comanda.fecha.toDate())
        holder.mesa.text = "Mesa ${comanda.numeroMesa}"
        holder.precio.text = "%.2f €".format(comanda.precio)

        // Trigger click callback
        holder.itemView.setOnClickListener {
            onClick(comanda)
        }
    }

    // Return the total number of items in the list
    override fun getItemCount(): Int = comandas.size
}
