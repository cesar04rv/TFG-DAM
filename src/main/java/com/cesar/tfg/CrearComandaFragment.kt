package com.cesar.tfg

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Controller.ResumenPlatosAdapter
import com.cesar.tfg.Model.Plato
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CrearComandaFragment : Fragment() {

    // UI components
    private lateinit var recyclerResumen: RecyclerView
    private lateinit var platosContainer: FrameLayout
    private lateinit var txtNumeroMesa: TextView
    private lateinit var btnVolver: Button
    private lateinit var btnCrearComanda: Button

    // Firebase references
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Data variables
    private var numeroMesa: String = ""
    private val platosSeleccionados = LinkedHashMap<Plato, Int>() // Stores selected dishes and their quantities
    private lateinit var adapterResumen: ResumenPlatosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crear_comanda, container, false)

        // Initialize views
        recyclerResumen = view.findViewById(R.id.recyclerResumen)
        platosContainer = view.findViewById(R.id.platosContainer)
        txtNumeroMesa = view.findViewById(R.id.txtNumeroMesa)
        btnVolver = view.findViewById(R.id.btnVolver)
        btnCrearComanda = view.findViewById(R.id.btnFinalizarComanda)

        // Set click listener for "back" button
        btnVolver.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Salir?")
                .setMessage("¿Seguro que quieres salir? La comanda no se guardará.")
                .setPositiveButton("Aceptar") { _, _ ->
                    findNavController().navigateUp()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Set click listener to save the order
        btnCrearComanda.setOnClickListener {
            guardarComanda()
        }

        // Initialize RecyclerView for order summary
        recyclerResumen.layoutManager = LinearLayoutManager(requireContext())
        adapterResumen = ResumenPlatosAdapter(emptyList()) { plato ->
            // Remove or decrease quantity of a dish when clicked
            val cantidad = platosSeleccionados[plato] ?: return@ResumenPlatosAdapter
            if (cantidad <= 1) {
                platosSeleccionados.remove(plato)
            } else {
                platosSeleccionados[plato] = cantidad - 1
            }
            actualizarResumen()
        }
        recyclerResumen.adapter = adapterResumen

        // Prompt user for table number
        pedirNumeroMesa()
        return view
    }

    // Updates the order summary list
    private fun actualizarResumen() {
        val resumenOrdenado = platosSeleccionados.entries
            .toList()
            .map { it.key to it.value }

        adapterResumen = ResumenPlatosAdapter(resumenOrdenado) { plato ->
            val cantidad = platosSeleccionados[plato] ?: return@ResumenPlatosAdapter
            if (cantidad <= 1) {
                platosSeleccionados.remove(plato)
            } else {
                platosSeleccionados[plato] = cantidad - 1
            }
            actualizarResumen()
        }
        recyclerResumen.adapter = adapterResumen
        recyclerResumen.scrollToPosition(0)
    }

    // Shows a dialog to input table number
    private fun pedirNumeroMesa() {
        val input = TextInputEditText(requireContext()).apply {
            hint = "Número de mesa"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val inputLayout = TextInputLayout(requireContext()).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = android.graphics.Color.TRANSPARENT
            setPadding(24, 0, 24, 0)
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Comanda")
            .setMessage("Introduce el número de mesa:")
            .setView(inputLayout)
            .setCancelable(false)
            .setNegativeButton("Cancelar") { _, _ ->
                findNavController().navigateUp()
            }
            .setPositiveButton("Aceptar", null)
            .create()

        dialog.setOnShowListener {
            val btnAceptar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnAceptar.setOnClickListener {
                numeroMesa = input.text.toString().trim()
                val numeroMesaInt = numeroMesa.toIntOrNull()

                if (numeroMesaInt == null) {
                    inputLayout.error = "Numero de mesa inválido"
                    return@setOnClickListener
                } else {
                    inputLayout.error = null
                }

                lifecycleScope.launch {
                    try {
                        // Check if table already has an open order
                        val mesaQuery = db.collection("mesa")
                            .whereEqualTo("numero_mesa", numeroMesaInt)
                            .get().await()

                        val mesaDoc = mesaQuery.documents.firstOrNull()

                        if (mesaDoc != null) {
                            val mesaRef = db.collection("mesa").document(mesaDoc.id)

                            val comandaActiva = db.collection("comanda")
                                .whereEqualTo("estado", "abierta")
                                .whereEqualTo("id_mesa", mesaRef)
                                .get().await()

                            if (!comandaActiva.isEmpty) {
                                dialog.dismiss()
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Mesa Ocupada")
                                    .setMessage("Ya hay una comanda abierta para la mesa $numeroMesaInt.")
                                    .setPositiveButton("OK") { _, _ ->
                                        findNavController().navigateUp()
                                    }
                                    .show()
                                return@launch
                            }
                        }

                        txtNumeroMesa.text = "Mesa $numeroMesa"
                        dialog.dismiss()
                        cargarCategorias()

                    } catch (e: Exception) {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Error al comprobar la mesa", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }

        dialog.show()
    }

    // Loads category buttons dynamically
    private fun cargarCategorias() {
        val categorias = listOf("BEBIDAS", "ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES")
        val grid = GridLayout(requireContext()).apply { columnCount = 4 }

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

    // Loads dishes of a selected category
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
                        categoria = it.getString("categoria") ?: ""
                    )
                }

                val grid = GridLayout(requireContext()).apply { columnCount = 4 }

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
                            val cantidadActual = platosSeleccionados[plato] ?: 0
                            platosSeleccionados.remove(plato)

                            // Add selected dish to top of the list
                            val nuevoMapa = linkedMapOf<Plato, Int>()
                            nuevoMapa[plato] = cantidadActual + 1
                            for ((p, c) in platosSeleccionados) {
                                nuevoMapa[p] = c
                            }

                            platosSeleccionados.clear()
                            platosSeleccionados.putAll(nuevoMapa)
                            actualizarResumen()
                        }
                    }
                    grid.addView(card)
                }

                val scroll = ScrollView(requireContext()).apply { addView(grid) }

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
                Toast.makeText(requireContext(), "Error al cargar los platos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Saves the order into Firestore
    private fun guardarComanda() {
        if (platosSeleccionados.isEmpty()) {
            Toast.makeText(requireContext(), "Añade al menos un plato", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: return@launch
                val trabajadorRef = db.collection("trabajador").document(user.uid)
                val trabajadorDoc = trabajadorRef.get().await()
                val nombreCompleto = trabajadorDoc.getString("nombre") + " " + trabajadorDoc.getString("apellidos")

                // Check if table already exists, else create it
                val mesaSnapshot = db.collection("mesa")
                    .whereEqualTo("numero_mesa", numeroMesa.toInt())
                    .get().await()

                val mesaRef = if (mesaSnapshot.isEmpty) {
                    val nuevaMesa = db.collection("mesa").document()
                    nuevaMesa.set(mapOf("numero_mesa" to numeroMesa.toInt())).await()
                    nuevaMesa
                } else {
                    mesaSnapshot.documents.first().reference
                }

                val precioTotal = platosSeleccionados.entries.sumOf { (plato, cantidad) -> plato.precio * cantidad }

                // Create the order in Firestore
                val comandaRef = db.collection("comanda").document()
                comandaRef.set(
                    mapOf(
                        "numero_comanda" to System.currentTimeMillis(),
                        "fecha" to Timestamp.now(),
                        "estado" to "abierta",
                        "precio" to precioTotal,
                        "id_mesa" to mesaRef,
                        "id_trabajador" to trabajadorRef,
                        "nombre_trabajador" to nombreCompleto
                    )
                ).await()

                // Save each dish related to the order
                platosSeleccionados.forEach { (plato, cantidad) ->
                    db.collection("comanda_plato").add(
                        mapOf(
                            "id_comanda" to comandaRef,
                            "id_plato" to db.collection("plato").document(plato.id),
                            "cantidad" to cantidad,
                            "nombre_plato" to plato.nombre,
                            "precio_plato" to plato.precio
                        )
                    ).await()
                }

                Toast.makeText(requireContext(), "Comanda creada", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
