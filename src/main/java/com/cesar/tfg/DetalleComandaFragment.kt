package com.cesar.tfg

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class DetalleComandaFragment : Fragment() {

    // UI elements
    private lateinit var tvTrabajador: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvTotal: TextView
    private lateinit var recycler: RecyclerView

    // List to hold dish information: name, quantity, and price
    private val platos = mutableListOf<Triple<String, Int, Double>>()

    // Firestore database reference
    private val db = Firebase.firestore

    // Order ID passed via arguments
    private lateinit var comandaId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the order ID from arguments
        arguments?.let {
            comandaId = it.getString("comandaId") ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_detalle_comanda, container, false)

        // Initialize views
        tvTrabajador = view.findViewById(R.id.tvTrabajadorDetalle)
        tvFecha = view.findViewById(R.id.tvFechaDetalle)
        tvTotal = view.findViewById(R.id.tvTotalDetalle)
        recycler = view.findViewById(R.id.recyclerPlatosDetalle)

        // Set back button click to return to previous screen
        view.findViewById<Button>(R.id.btnVolver).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Set up the RecyclerView with a simple adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = object : RecyclerView.Adapter<PlatoViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatoViewHolder {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_plato_comanda, parent, false)
                return PlatoViewHolder(item)
            }

            override fun onBindViewHolder(holder: PlatoViewHolder, position: Int) {
                val (nombre, cantidad, precio) = platos[position]

                // Set the dish name and its quantity and price
                holder.nombre.text = nombre
                holder.info.text = "x$cantidad   %.2f€".format(precio)

                // Hide delete button if present in layout (not needed in detail view)
                holder.itemView.findViewById<View>(R.id.btnEliminarPlato)?.visibility = View.GONE
            }

            override fun getItemCount(): Int = platos.size
        }

        // Load data for the selected order
        cargarDatos()
        return view
    }

    // Loads the order and its related dish data from Firestore
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                // Fetch order document
                val doc = db.collection("comanda").document(comandaId).get().await()

                val nombre = doc.getString("nombre_trabajador") ?: "Unknown"
                val fecha = doc.getTimestamp("fecha") ?: Timestamp.now()
                val total = doc.getDouble("precio") ?: 0.0

                // Format date to readable format
                val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                // Display general order information
                tvTrabajador.text = "Trabajador: $nombre"
                tvFecha.text = "Fecha: ${formato.format(fecha.toDate())}"
                tvTotal.text = "Total: %.2f €".format(total)

                // Fetch all dishes related to this order from comanda_plato collection
                val platosSnapshot = db.collection("comanda_plato")
                    .whereEqualTo("id_comanda", db.collection("comanda").document(comandaId))
                    .get().await()

                // Clear the current list and populate with retrieved data
                platos.clear()
                for (doc in platosSnapshot.documents) {
                    val nombre = doc.getString("nombre_plato") ?: "Dish"
                    val precio = doc.getDouble("precio_plato") ?: 0.0
                    val cantidad = doc.getLong("cantidad")?.toInt() ?: 1
                    platos.add(Triple(nombre, cantidad, precio))
                }

                // Notify adapter to refresh the RecyclerView
                recycler.adapter?.notifyDataSetChanged()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar los detalles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ViewHolder to bind each dish row in the RecyclerView
    class PlatoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.tvNombrePlato)
        val info: TextView = view.findViewById(R.id.tvCantidadPrecio)
    }
}
