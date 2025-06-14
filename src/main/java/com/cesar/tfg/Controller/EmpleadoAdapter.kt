package com.cesar.tfg.Controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.R
import com.cesar.tfg.Model.Empleado

// Adapter used to display and manage a list of employees, with support for edit/delete actions
class EmpleadoAdapter(
    private var empleados: List<Empleado>,
    private val modo: Modo, // Determines whether the adapter is in EDIT or DELETE mode
    private val onAccionClick: (Empleado) -> Unit // Callback triggered when the action button is pressed
) : RecyclerView.Adapter<EmpleadoAdapter.EmpleadoViewHolder>() {

    // Enum representing the adapter's mode: edit or delete
    enum class Modo {
        EDITAR, ELIMINAR
    }

    // ViewHolder holding references to the UI elements for a single employee item
    inner class EmpleadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.textNombreEmpleado)  // Employee's name
        val email: TextView = view.findViewById(R.id.textEmailEmpleado)    // Employee's email
        val jefe: TextView = view.findViewById(R.id.textEsJefe)            // "Jefe" label if the employee is a manager
        val btnAccion: Button = view.findViewById(R.id.btnAccionEmpleado)  // Button to edit or delete
    }

    // Inflate the item layout and return a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_empleado, parent, false)
        return EmpleadoViewHolder(view)
    }

    // Bind employee data to the UI elements of the ViewHolder
    override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
        val empleado = empleados[position]
        holder.nombre.text = empleado.nombre.replaceFirstChar { it.uppercaseChar() }
        holder.email.text = empleado.email
        holder.jefe.visibility = if (empleado.esJefe) View.VISIBLE else View.GONE
        holder.btnAccion.text = if (modo == Modo.EDITAR) "Editar" else "Eliminar"

        // Handle action button click (edit or delete)
        holder.btnAccion.setOnClickListener {
            onAccionClick(empleado)
        }
    }

    // Return total number of employee items
    override fun getItemCount(): Int = empleados.size

    // Update the adapter's employee list and refresh the view
    fun actualizar(nuevaLista: List<Empleado>) {
        empleados = nuevaLista
        notifyDataSetChanged()
    }
}
