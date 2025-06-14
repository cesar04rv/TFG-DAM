package com.cesar.tfg.Controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.R
import com.cesar.tfg.Model.Plato
import com.cesar.tfg.Model.PlatoItem

// Adapter to display and manage a categorized, collapsible list of dishes (Platos)
class PlatoAdapter(
    private val modo: Modo, // Determines if we're in EDITAR (edit) or ELIMINAR (delete) mode
    private val onAccionClick: (Plato) -> Unit // Callback when edit/delete button is clicked
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Modes for adapter behavior
    enum class Modo {
        EDITAR, ELIMINAR
    }

    private val platosOriginales = mutableListOf<Plato>() // Full list of platos
    private val categoriaExpandida = mutableMapOf<String, Boolean>() // Expansion state per category
    private val items = mutableListOf<PlatoItem>() // Final flattened list with headers and data items

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PLATO = 1
    }

    // Update the adapter with a new list of platos
    fun actualizar(platos: List<Plato>) {
        platosOriginales.clear()
        platosOriginales.addAll(platos)

        // Mark each category as collapsed by default if not seen before
        val categorias = listOf("ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES", "BEBIDAS")
        for (categoria in categorias) {
            if (categoriaExpandida[categoria] == null) {
                categoriaExpandida[categoria] = false
            }
        }

        reconstruirItems()
    }

    // Rebuild the items list including headers and their respective platos
    private fun reconstruirItems() {
        items.clear()
        val categorias = listOf("ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES", "BEBIDAS")

        for (categoria in categorias) {
            val platosCategoria = platosOriginales
                .filter { it.categoria == categoria }
                .sortedBy { it.nombre.lowercase() }

            if (platosCategoria.isNotEmpty()) {
                items.add(PlatoItem.Header(categoria, categoriaExpandida[categoria] == true))
                if (categoriaExpandida[categoria] == true) {
                    items.addAll(platosCategoria.map { PlatoItem.PlatoData(it) })
                }
            }
        }

        notifyDataSetChanged()
    }

    // Return view type depending on whether it's a header or a plato
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PlatoItem.Header -> VIEW_TYPE_HEADER
            is PlatoItem.PlatoData -> VIEW_TYPE_PLATO
        }
    }

    override fun getItemCount(): Int = items.size

    // Inflate the correct layout depending on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_header_categoria, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plato, parent, false)
            PlatoViewHolder(view)
        }
    }

    // Bind data to the appropriate ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PlatoItem.Header -> (holder as HeaderViewHolder).bind(item)
            is PlatoItem.PlatoData -> (holder as PlatoViewHolder).bind(item.plato)
        }
    }

    // ViewHolder for category headers
    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvCategoria: TextView = view.findViewById(R.id.tvCategoriaHeader)
        private val iconExpand: ImageView = view.findViewById(R.id.iconExpand)

        fun bind(header: PlatoItem.Header) {
            tvCategoria.text = header.categoria
            iconExpand.setImageResource(
                if (header.expandido) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
            )

            // Toggle expansion state when header is clicked
            itemView.setOnClickListener {
                header.expandido = !header.expandido
                iconExpand.setImageResource(
                    if (header.expandido) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
                )
                toggleCategoria(header.categoria, header.expandido)
            }
        }
    }

    // Toggle a category's expanded/collapsed state and refresh the list
    private fun toggleCategoria(categoria: String, expandido: Boolean) {
        categoriaExpandida[categoria] = expandido
        reconstruirItems()
    }

    // ViewHolder for individual platos
    inner class PlatoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        private val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        private val btnAccion: Button = view.findViewById(R.id.btnAccion)

        fun bind(plato: Plato) {
            tvNombre.text = plato.nombre.replaceFirstChar { it.uppercase() }
            tvPrecio.text = "${plato.precio} €"
            btnAccion.text = if (modo == Modo.EDITAR) "Editar" else "Eliminar"
            btnAccion.setOnClickListener { onAccionClick(plato) }
        }
    }
}
