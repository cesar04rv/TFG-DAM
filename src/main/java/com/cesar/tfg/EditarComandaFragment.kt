package com.cesar.tfg

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Controller.ResumenPlatosEditarAdapter
import com.cesar.tfg.Model.PlatoEditado
import com.cesar.tfg.Model.Plato
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditarComandaFragment : Fragment() {

    private lateinit var recyclerResumen: RecyclerView
    private lateinit var platosContainer: FrameLayout
    private lateinit var btnGuardarCambios: Button
    private lateinit var btnVolver: Button
    private lateinit var txtNumeroMesa: TextView

    private val db = Firebase.firestore
    private lateinit var comandaId: String
    private val platosSeleccionados = mutableListOf<PlatoEditado>()
    private lateinit var adapterResumen: ResumenPlatosEditarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_editar_comanda, container, false)

        // Initialize UI components
        recyclerResumen = view.findViewById(R.id.recyclerResumen)
        platosContainer = view.findViewById(R.id.platosContainer)
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios)
        btnVolver = view.findViewById(R.id.btnVolver)
        txtNumeroMesa = view.findViewById(R.id.txtNumeroMesa)

        // Retrieve arguments using Safe Args
        val args = EditarComandaFragmentArgs.fromBundle(requireArguments())
        comandaId = args.comandaId

        // Show confirmation dialog before exiting
        btnVolver.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Salir?")
                .setMessage("¿Seguro que quieres salir? Los cambios no se guardarán.")
                .setPositiveButton("Aceptar") { _, _ -> findNavController().navigateUp() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Save changes button
        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        // Setup RecyclerView
        recyclerResumen.layoutManager = LinearLayoutManager(requireContext())
        adapterResumen = ResumenPlatosEditarAdapter(platosSeleccionados) { platoEditado ->
            // Reduce quantity depending on whether it's from original or new round
            if (platoEditado.cantidadRonda > 0) {
                platoEditado.cantidadRonda--
                if (platoEditado.cantidadOriginal == 0 && platoEditado.cantidadRonda == 0) {
                    platosSeleccionados.remove(platoEditado)
                }
            } else if (platoEditado.cantidadOriginal > 1) {
                platoEditado.cantidadOriginal--
            } else if (platoEditado.cantidadOriginal == 1 && platoEditado.cantidadRonda == 0) {
                platosSeleccionados.remove(platoEditado)
            }
            adapterResumen.notifyDataSetChanged()
        }
        recyclerResumen.adapter = adapterResumen

        // Load initial data
        cargarNumeroMesa()
        cargarPlatosDeLaComanda()
        cargarCategorias()

        return view
    }

    // Load table number linked to the current order
    private fun cargarNumeroMesa() {
        lifecycleScope.launch {
            try {
                val comandaDoc = db.collection("comanda").document(comandaId).get().await()
                val mesaRef = comandaDoc.getDocumentReference("id_mesa")
                val mesaDoc = mesaRef?.get()?.await()
                val numeroMesa = mesaDoc?.getLong("numero_mesa")?.toInt()
                txtNumeroMesa.text = "Mesa ${numeroMesa ?: "-"}"
            } catch (e: Exception) {
                txtNumeroMesa.text = "Mesa -"
            }
        }
    }

    // Load the selected dishes from the order
    private fun cargarPlatosDeLaComanda() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("comanda_plato")
                    .whereEqualTo("id_comanda", db.collection("comanda").document(comandaId))
                    .get().await()

                // Loop through all documents to reconstruct the selected dishes
                for (doc in snapshot.documents) {
                    val platoRef = doc.getDocumentReference("id_plato") ?: continue
                    val platoSnap = platoRef.get().await()
                    val plato = Plato(
                        id = platoRef.id,
                        nombre = platoSnap.getString("nombre") ?: "",
                        precio = platoSnap.getDouble("precio") ?: 0.0,
                        categoria = platoSnap.getString("categoria") ?: "",
                        activo = platoSnap.getBoolean("activo") ?: true
                    )
                    val cantidad = doc.getLong("cantidad")?.toInt() ?: 1
                    platosSeleccionados.add(PlatoEditado(plato, cantidad, 0))
                }

                // Order dishes by category
                val ordenCategorias = listOf("BEBIDAS", "ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES")
                platosSeleccionados.sortWith(compareBy {
                    ordenCategorias.indexOf(it.plato.categoria.uppercase())
                })
                adapterResumen.notifyDataSetChanged()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar los platos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Display dish categories as cards
    private fun cargarCategorias() {
        val categorias = listOf("BEBIDAS", "ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES")
        val grid = GridLayout(requireContext()).apply {
            columnCount = 4
        }

        for (categoria in categorias) {
            val card = MaterialCardView(requireContext()).apply {
                val text = TextView(context).apply {
                    text = categoria
                    gravity = Gravity.CENTER
                    setPadding(16, 16, 16, 16)
                }
                addView(text)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 300
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                setOnClickListener {
                    mostrarPlatosPorCategoria(categoria)
                }
            }
            grid.addView(card)
        }

        val scroll = ScrollView(requireContext())
        scroll.addView(grid)
        platosContainer.removeAllViews()
        platosContainer.addView(scroll)
    }

    // Show dishes belonging to a selected category
    private fun mostrarPlatosPorCategoria(categoria: String) {
        lifecycleScope.launch {
            try {
                val result = db.collection("plato")
                    .whereEqualTo("categoria", categoria)
                    .whereEqualTo("activo", true)
                    .get().await()

                val lista = result.map {
                    Plato(
                        id = it.id,
                        nombre = it.getString("nombre") ?: "",
                        precio = it.getDouble("precio") ?: 0.0,
                        categoria = it.getString("categoria") ?: "",
                        activo = it.getBoolean("activo") ?: true
                    )
                }

                val grid = GridLayout(requireContext()).apply {
                    columnCount = 4
                }

                // Create card for each dish
                for (plato in lista) {
                    val card = MaterialCardView(requireContext()).apply {
                        val text = TextView(context).apply {
                            text = "${plato.nombre}\n${plato.precio}€"
                            gravity = Gravity.CENTER
                            setPadding(16, 16, 16, 16)
                        }
                        addView(text)
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = 300
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            setMargins(8, 8, 8, 8)
                        }
                        setOnClickListener {
                            val existente = platosSeleccionados.find { it.plato.id == plato.id }
                            if (existente != null) {
                                existente.cantidadRonda++
                                platosSeleccionados.remove(existente)
                                platosSeleccionados.add(0, existente)
                            } else {
                                platosSeleccionados.add(0, PlatoEditado(plato, 0, 1))
                            }
                            adapterResumen.notifyDataSetChanged()
                            recyclerResumen.scrollToPosition(0)
                        }
                    }
                    grid.addView(card)
                }

                val scroll = ScrollView(requireContext()).apply {
                    addView(grid)
                }

                val layoutFinal = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    val btnVolverCat = Button(requireContext()).apply {
                        text = "Volver"
                        setTextColor(resources.getColor(R.color.white, null))
                        background = resources.getDrawable(R.drawable.btn_volver_background, null)
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        gravity = Gravity.CENTER
                        elevation = 8f
                        setPadding(32, 16, 32, 16)
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow_white, 0, 0, 0)
                        compoundDrawablePadding = 16
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(24, 24, 0, 16)
                        }
                        setOnClickListener { cargarCategorias() }
                    }
                    addView(btnVolverCat)
                    addView(scroll)
                }

                platosContainer.removeAllViews()
                platosContainer.addView(layoutFinal)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar los paltos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save all the selected changes to Firestore
    private fun guardarCambios() {
        lifecycleScope.launch {
            try {
                val platosValidos = platosSeleccionados.filter {
                    it.cantidadOriginal + it.cantidadRonda > 0
                }

                // Prevent saving an empty order
                if (platosValidos.isEmpty()) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Comanda vacía")
                        .setMessage("No puedes guardar una comanda sin platos.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                // Delete existing dish relationships for the order
                val snapshot = db.collection("comanda_plato")
                    .whereEqualTo("id_comanda", db.collection("comanda").document(comandaId))
                    .get().await()

                for (doc in snapshot.documents) {
                    db.collection("comanda_plato").document(doc.id).delete().await()
                }

                // Recreate dish relationships with updated quantities
                for (platoEditado in platosValidos) {
                    val total = platoEditado.cantidadOriginal + platoEditado.cantidadRonda
                    val rel = mapOf(
                        "id_comanda" to db.collection("comanda").document(comandaId),
                        "id_plato" to db.collection("plato").document(platoEditado.plato.id),
                        "cantidad" to total,
                        "nombre_plato" to platoEditado.plato.nombre,
                        "precio_plato" to platoEditado.plato.precio
                    )
                    db.collection("comanda_plato").add(rel).await()
                }

                // Recalculate and update the total price
                val precioTotal = platosValidos.sumOf {
                    (it.cantidadOriginal + it.cantidadRonda) * it.plato.precio
                }

                db.collection("comanda").document(comandaId)
                    .update("precio", precioTotal).await()

                Toast.makeText(requireContext(), "Comanda actualizada", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
