package com.cesar.tfg

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Controller.ComandaContabilidadAdapter
import com.cesar.tfg.Model.Comanda
import com.cesar.tfg.Model.ContabilidadViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class ContabilidadPorFechasFragment : Fragment() {

    private lateinit var btnSeleccionarRango: Button
    private lateinit var tvIngresosTotales: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var dropdownEmpleado: AutoCompleteTextView
    private lateinit var adapter: ComandaContabilidadAdapter

    private val db = Firebase.firestore
    private val listaComandas = mutableListOf<Comanda>()

    private val listaNombres = mutableListOf<String>()
    private val mapaEmpleados = mutableMapOf<String, DocumentReference>()

    private val viewModel: ContabilidadViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_contabilidad_por_fechas, container, false)

        // UI references
        btnSeleccionarRango = view.findViewById(R.id.btnSeleccionarRango)
        tvIngresosTotales = view.findViewById(R.id.tvIngresosTotales)
        recycler = view.findViewById(R.id.recyclerContabilidad)
        dropdownEmpleado = view.findViewById(R.id.dropdownEmpleado)

        // Set up RecyclerView
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ComandaContabilidadAdapter(listaComandas) { comanda ->
            // Navigate to detail fragment when an item is clicked
            val action = GestionNavGraphDirections.actionGlobalDetalleComandaFragment(comanda.id)
            parentFragment?.findNavController()?.navigate(action)
        }
        recycler.adapter = adapter

        // Date range picker button
        btnSeleccionarRango.setOnClickListener { mostrarDatePicker() }

        // Set up dropdown menu for employees
        val dropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listaNombres)
        dropdownEmpleado.setAdapter(dropdownAdapter)

        // Handle employee selection
        dropdownEmpleado.setOnItemClickListener { _, _, position, _ ->
            val nombreSeleccionado = listaNombres[position]
            viewModel.referenciaEmpleadoSeleccionado = if (nombreSeleccionado == "Todos") null else mapaEmpleados[nombreSeleccionado]
            viewModel.nombreEmpleadoSeleccionado = nombreSeleccionado
        }

        // Load employees from Firestore
        cargarEmpleados()

        // Restore previous employee selection if available
        viewModel.nombreEmpleadoSeleccionado?.let {
            dropdownEmpleado.setText(it, false)
        }

        // Search button to load data based on filters
        val btnBuscar = view.findViewById<Button>(R.id.btnBuscar)
        btnBuscar.setOnClickListener {
            val nombreSeleccionado = dropdownEmpleado.text.toString()
            viewModel.referenciaEmpleadoSeleccionado = if (nombreSeleccionado == "Todos") null else mapaEmpleados[nombreSeleccionado]
            viewModel.nombreEmpleadoSeleccionado = nombreSeleccionado

            // Validate date range selection
            if (viewModel.fechaInicio == null || viewModel.fechaFin == null) {
                Toast.makeText(requireContext(), "Por favor selecciona un rango de fecha", Toast.LENGTH_SHORT).show()
            } else {
                cargarComandas(viewModel.fechaInicio!!, viewModel.fechaFin!!)
            }
        }

        // Automatically reload if dates are already selected
        if (viewModel.fechaInicio != null && viewModel.fechaFin != null) {
            cargarComandas(viewModel.fechaInicio!!, viewModel.fechaFin!!)
        }

        return view
    }

    // Load employee names and references from Firestore
    private fun cargarEmpleados() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("trabajador").get().await()

                listaNombres.clear()
                mapaEmpleados.clear()
                listaNombres.add("Todos")

                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No hay empleados que coincidan con ese usuario", Toast.LENGTH_SHORT).show()
                }

                // Populate name list and reference map
                for (doc in snapshot.documents) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""
                    val nombreCompleto = "$nombre $apellidos"
                    val referencia = doc.reference

                    println("Trabajador cargado: $nombreCompleto -> ${referencia.path}")

                    listaNombres.add(nombreCompleto)
                    mapaEmpleados[nombreCompleto] = referencia
                }

                // Notify dropdown adapter of the update
                (dropdownEmpleado.adapter as ArrayAdapter<*>).notifyDataSetChanged()

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Toast.makeText(requireContext(), "Error al cargar empleados: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    // Show the Material Date Picker to select a date range
    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Selecciona un rango de fechas")
            .setTheme(R.style.ThemeOverlay_MyDatePicker)
            .build()

        picker.show(parentFragmentManager, "rango")

        picker.addOnPositiveButtonClickListener { seleccion ->
            viewModel.fechaInicio = Timestamp(Date(seleccion.first))
            viewModel.fechaFin = Timestamp(Date(seleccion.second + (24 * 60 * 60 * 1000 - 1))) // include full day
        }
    }

    // Load filtered orders based on selected employee and date range
    private fun cargarComandas(inicio: Timestamp, fin: Timestamp) {
        lifecycleScope.launch {
            try {
                val refSeleccionada = viewModel.referenciaEmpleadoSeleccionado

                var query = db.collection("comanda")
                    .whereGreaterThanOrEqualTo("fecha", inicio)
                    .whereLessThanOrEqualTo("fecha", fin)
                    .whereEqualTo("estado", "finalizada")

                // Filter by employee if selected
                if (refSeleccionada != null) {
                    query = query.whereEqualTo("id_trabajador", refSeleccionada)
                }

                val snapshot = query.orderBy("fecha", Query.Direction.DESCENDING).get().await()

                listaComandas.clear()
                var total = 0.0

                // Process results and build Comanda objects
                for (doc in snapshot.documents) {
                    val nombreTrabajador = doc.getString("nombre_trabajador") ?: ""
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
                        nombreTrabajador = nombreTrabajador
                    )

                    listaComandas.add(comanda)
                    total += comanda.precio
                }

                // Display total income
                tvIngresosTotales.text = "Total recaudado: %.2f €".format(total)
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
}
