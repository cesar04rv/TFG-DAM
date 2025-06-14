package com.cesar.tfg.Controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Model.Comanda
import com.cesar.tfg.R
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

// Adapter to display a list of comandas belonging to the logged-in employee
class MisComandasAdapter(
    private val comandas: List<Comanda>,
    private val onFinalizarClick: (Comanda) -> Unit, // Callback for 'Finalize' button
    private val onEditarClick: (Comanda) -> Unit,    // Callback for 'Edit' button
    private val onCuentaClick: (Comanda) -> Unit,    // Callback for 'Cuenta' (Send bill) button
    private val mostrarBotones: Boolean              // Flag to control which buttons are visible
) : RecyclerView.Adapter<MisComandasAdapter.ViewHolder>() {

    // ViewHolder containing all UI elements of a comanda item
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val texto: TextView = view.findViewById(R.id.tvResumenComanda)   // Summary text (worker, table, time, state, price)
        val btnFinalizar: Button = view.findViewById(R.id.btnFinalizar)  // Finalize button
        val btnEditar: Button = view.findViewById(R.id.btnEditar)        // Edit button
        val btnCuenta: Button = view.findViewById(R.id.btnCuenta)        // Send bill button
    }

    // Inflate the layout for each item and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mi_comanda, parent, false)
        return ViewHolder(view)
    }

    // Return the total number of comandas
    override fun getItemCount(): Int = comandas.size

    // Bind the comanda data to the ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comanda = comandas[position]

        val hora = formatHora(comanda.fecha)
        val mesa = comanda.numeroMesa
        val estado = comanda.estado.uppercase()
        val precio = String.format("%.2f", comanda.precio)
        val trabajador = comanda.nombreTrabajador

        // Set the formatted summary text
        holder.texto.text = "Trabajador: $trabajador\nMesa $mesa – $hora – $estado – $precio€"

        // Handle 'Cuenta' button click
        holder.btnCuenta.setOnClickListener {
            onCuentaClick(comanda)
        }

        // Depending on the mode, show edit/finalize buttons or only the 'Cuenta' button
        if (mostrarBotones) {
            holder.btnEditar.visibility = View.VISIBLE
            holder.btnFinalizar.visibility = View.VISIBLE
            holder.btnCuenta.visibility = View.GONE

            holder.btnEditar.text = "Editar"
            holder.btnFinalizar.text = "Finalizar"

            holder.btnEditar.setOnClickListener { onEditarClick(comanda) }
            holder.btnFinalizar.setOnClickListener { onFinalizarClick(comanda) }

        } else {
            holder.btnEditar.visibility = View.GONE
            holder.btnFinalizar.visibility = View.GONE
            holder.btnCuenta.visibility = View.VISIBLE
        }
    }

    // Helper function to format Timestamp to "HH:mm"
    private fun formatHora(fecha: Timestamp): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(fecha.toDate())
    }
}
