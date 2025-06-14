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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class ContabilidadHistoricoFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvIngresos: TextView
    private lateinit var btnToggle: Button
    private lateinit var adapter: ComandaContabilidadAdapter
    private val lista = mutableListOf<Comanda>()
    private val db = Firebase.firestore
    private var descendente = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_lista_comandas_contabilidad, container, false)

        tvIngresos = view.findViewById(R.id.tvIngresosTotales)
        recycler = view.findViewById(R.id.recyclerContabilidad)
        btnToggle = view.findViewById(R.id.btnToggleOrden)

        view.findViewById<Button>(R.id.btnFinalizarTodas).visibility = View.GONE

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ComandaContabilidadAdapter(lista) { comanda ->
            val action = GestionNavGraphDirections.actionGlobalDetalleComandaFragment(comanda.id)
            parentFragment?.findNavController()?.navigate(action)
        }
        recycler.adapter = adapter

        btnToggle.visibility = View.VISIBLE
        btnToggle.setOnClickListener {
            descendente = !descendente
            cargarComandas()
        }

        cargarComandas()
        return view
    }


    // Load all finalized orders from Firestore, sorted by date
    private fun cargarComandas() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("comanda")
                    .whereEqualTo("estado", "finalizada")
                    .orderBy("fecha", if (descendente) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
                    .get().await()

                lista.clear()
                var total = 0.0

                // Iterate through each document and build the list of orders
                for (doc in snapshot.documents) {
                    val mesaRef = doc.getDocumentReference("id_mesa")
                    val mesaNumero = mesaRef?.get()?.await()?.getLong("numero_mesa") ?: 0

                    // Build Comanda object from Firestore document
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
                    total += comanda.precio // Accumulate total income
                }

                // Display total income
                tvIngresos.text = "Ingresos totales: %.2f €".format(total)
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                // Handle only unexpected exceptions (excluding coroutine cancellations)
                if (e !is CancellationException) {
                    Toast.makeText(requireContext(), "Error al cargar comandas", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
}
