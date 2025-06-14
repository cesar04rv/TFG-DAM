package com.cesar.tfg

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Controller.ComandaContabilidadAdapter
import com.cesar.tfg.Model.Comanda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class ContabilidadHoyFragment : Fragment() {

    private lateinit var tvIngresos: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ComandaContabilidadAdapter
    private lateinit var btnFinalizarTodas: Button
    private val lista = mutableListOf<Comanda>()
    private val db = Firebase.firestore
    private var ordenAscendente = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_lista_comandas_contabilidad, container, false)

        val btnToggleOrden = view.findViewById<Button>(R.id.btnToggleOrden)

        // Toggle sorting order when clicked
        btnToggleOrden.setOnClickListener {
            alternarOrden()
        }

        tvIngresos = view.findViewById(R.id.tvIngresosTotales)
        recycler = view.findViewById(R.id.recyclerContabilidad)
        btnFinalizarTodas = view.findViewById(R.id.btnFinalizarTodas)

        // Set up RecyclerView and adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ComandaContabilidadAdapter(lista) { comanda ->
            // Navigate to detailed order screen
            val action = GestionNavGraphDirections.actionGlobalDetalleComandaFragment(comanda.id)
            parentFragment?.findNavController()?.navigate(action)
        }
        recycler.adapter = adapter

        // Load initial sorting order (default or passed via arguments)
        ordenAscendente = arguments?.getBoolean("ordenAscendente") ?: false
        cargarComandas()

        // Button to finalize all active orders
        btnFinalizarTodas.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmación")
                .setMessage("¿Seguro que quieres finalizar todas las comandas?")
                .setPositiveButton("Yes") { _, _ ->
                    finalizarComandasNoFinalizadas()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }

    // Load finalized orders for the current day and calculate total income
    private fun cargarComandas() {
        lifecycleScope.launch {
            try {
                // Define start and end timestamps for today
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val inicio = Timestamp(calendar.time)
                val fin = Timestamp(Date(inicio.toDate().time + 24 * 60 * 60 * 1000 - 1))

                // Query finalized orders within today's date range
                val snapshot = db.collection("comanda")
                    .whereGreaterThanOrEqualTo("fecha", inicio)
                    .whereLessThanOrEqualTo("fecha", fin)
                    .whereEqualTo("estado", "finalizada")
                    .orderBy("fecha", if (ordenAscendente) Query.Direction.ASCENDING else Query.Direction.DESCENDING)
                    .get().await()

                lista.clear()
                var total = 0.0

                // Build list of orders from query results
                for (doc in snapshot.documents) {
                    val mesaRef = doc.getDocumentReference("id_mesa")
                    val mesaNumero = mesaRef?.get()?.await()?.getLong("numero_mesa") ?: 0
                    val comanda = Comanda(
                        id = doc.id,
                        numero = doc.getLong("numero_comanda") ?: 0,
                        fecha = doc.getTimestamp("fecha") ?: Timestamp.now(),
                        estado = doc.getString("estado") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        idMesa = mesaRef,
                        numeroMesa = mesaNumero.toInt(),
                        nombreTrabajador = doc.getString("nombre_trabajador") ?: ""
                    )
                    lista.add(comanda)
                    total += comanda.precio
                }

                // Display total income
                tvIngresos.text = "Total recaudado: %.2f €".format(total)
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                // Handle only unexpected errors (excluding coroutine cancellations)
                if (e !is CancellationException) {
                    Toast.makeText(requireContext(), "Error al cargar las comandas", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    // Public method to update sorting order externally
    fun actualizarOrden(nuevoOrdenAscendente: Boolean) {
        ordenAscendente = nuevoOrdenAscendente
        cargarComandas()
    }

    // Toggle sorting order between ascending and descending
    fun alternarOrden() {
        ordenAscendente = !ordenAscendente
        cargarComandas()
    }

    // Finalize all orders that are still active
    private fun finalizarComandasNoFinalizadas() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("comanda")
                    .whereEqualTo("estado", "abierta")
                    .get().await()

                // Update each active order to finalized state
                for (doc in snapshot.documents) {
                    db.collection("comanda").document(doc.id)
                        .update("estado", "finalizada")
                        .await()
                }

                Toast.makeText(requireContext(), "Comandas activas finalizadas", Toast.LENGTH_SHORT).show()
                cargarComandas()

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Toast.makeText(requireContext(), "Error al finalizar las comandas", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
}
