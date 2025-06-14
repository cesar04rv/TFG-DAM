package com.cesar.tfg

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Controller.PlatoAdapter
import com.cesar.tfg.Controller.PlatoAdapter.Modo.*
import com.cesar.tfg.Model.Plato
import com.cesar.tfg.utils.configurarBarrasBlancas
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlatoFragment : Fragment() {

    // UI components
    private lateinit var btnCrear: Button
    private lateinit var btnEditar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnVolverGeneral: Button

    private lateinit var layoutCrear: LinearLayout
    private lateinit var layoutListadoPlatos: LinearLayout
    private lateinit var recyclerPlatosGlobal: RecyclerView
    private lateinit var scrollCarta: ScrollView

    private lateinit var editNombre: EditText
    private lateinit var editPrecio: EditText
    private lateinit var editCategoria: EditText

    // Firebase
    private val db = Firebase.firestore

    // Track which dish is being edited
    private var platoEditando: Plato? = null

    // Valid category names
    private val categoriasValidas = listOf("ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES", "BEBIDAS")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_plato, container, false)
        configurarBarrasBlancas()

        // Bind views
        btnCrear = view.findViewById(R.id.btnCrearPlato)
        btnEditar = view.findViewById(R.id.btnEditarPlato)
        btnEliminar = view.findViewById(R.id.btnEliminarPlato)
        btnGuardar = view.findViewById(R.id.btnGuardarPlato)
        btnActualizar = view.findViewById(R.id.btnActualizarPlato)
        btnVolverGeneral = view.findViewById(R.id.btnVolverGeneral)

        layoutCrear = view.findViewById(R.id.layoutCrear)
        layoutListadoPlatos = view.findViewById(R.id.layoutListadoPlatos)
        recyclerPlatosGlobal = view.findViewById(R.id.recyclerPlatosGlobal)
        scrollCarta = view.findViewById(R.id.scrollCarta)

        editNombre = view.findViewById(R.id.editNombreCrear)
        editPrecio = view.findViewById(R.id.editPrecioCrear)
        editCategoria = view.findViewById(R.id.editCategoriaCrear)

        recyclerPlatosGlobal.layoutManager = LinearLayoutManager(requireContext())

        // Handle create button
        btnCrear.setOnClickListener { mostrarFormulario(false) }

        // Handle edit button
        btnEditar.setOnClickListener {
            ocultarTodo()
            btnVolverGeneral.visibility = View.VISIBLE
            recyclerPlatosGlobal.visibility = View.VISIBLE
            recyclerPlatosGlobal.adapter = PlatoAdapter(EDITAR) { mostrarEditar(it) }
            cargarPlatos()
        }

        // Handle delete button
        btnEliminar.setOnClickListener {
            ocultarTodo()
            btnVolverGeneral.visibility = View.VISIBLE
            recyclerPlatosGlobal.visibility = View.VISIBLE
            recyclerPlatosGlobal.adapter = PlatoAdapter(ELIMINAR) { eliminarPlato(it) }
            cargarPlatos()
        }

        // Handle back button
        btnVolverGeneral.setOnClickListener {
            if (vieneDeEditar) {
                vieneDeEditar = false
                ocultarTodo()
                btnVolverGeneral.visibility = View.VISIBLE
                recyclerPlatosGlobal.visibility = View.VISIBLE
                recyclerPlatosGlobal.adapter = PlatoAdapter(EDITAR) { mostrarEditar(it) }
                cargarPlatos()
            } else {
                mostrarListado()
            }
        }

        btnGuardar.setOnClickListener { guardarPlato() }
        btnActualizar.setOnClickListener { actualizarPlato() }

        mostrarListado()
        return view
    }

    // Displays the dish list grouped by category
    private fun mostrarListado() {
        ocultarTodo()
        scrollCarta.visibility = View.VISIBLE
        cargarPlatosAgrupados()
    }

    // Shows form for creating or editing a dish
    private fun mostrarFormulario(esEdicion: Boolean) {
        ocultarTodo()
        layoutCrear.visibility = View.VISIBLE
        btnVolverGeneral.visibility = View.VISIBLE
        btnGuardar.visibility = if (esEdicion) View.GONE else View.VISIBLE
        btnActualizar.visibility = if (esEdicion) View.VISIBLE else View.GONE
        if (!esEdicion) limpiarCampos()
    }

    // Hides all UI sections
    private fun ocultarTodo() {
        scrollCarta.visibility = View.GONE
        layoutCrear.visibility = View.GONE
        recyclerPlatosGlobal.visibility = View.GONE
        btnVolverGeneral.visibility = View.GONE
    }

    // Clears input fields
    private fun limpiarCampos() {
        editNombre.text.clear()
        editPrecio.text.clear()
        editCategoria.text.clear()
        platoEditando = null
    }

    private var vieneDeEditar: Boolean = false

    // Loads selected dish data into form for editing
    private fun mostrarEditar(plato: Plato) {
        vieneDeEditar = true
        platoEditando = plato
        mostrarFormulario(true)
        editNombre.setText(plato.nombre)
        editPrecio.setText(plato.precio.toString())
        editCategoria.setText(plato.categoria)

        editNombre.requestFocus()
        editNombre.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editNombre, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    // Adds a new dish to Firestore
    private fun guardarPlato() {
        val nombre = editNombre.text.toString().trim()
        val precio = editPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val categoria = editCategoria.text.toString().trim().uppercase()

        if (nombre.isEmpty() || categoria.isEmpty()) {
            mostrarError("Rellena todos los campos.")
            return
        }

        if (categoria !in categoriasValidas) {
            mostrarError("Categoría no válida.")
            return
        }

        lifecycleScope.launch {
            try {
                val plato = hashMapOf(
                    "nombre" to nombre,
                    "precio" to precio,
                    "categoria" to categoria,
                    "activo" to true
                )
                db.collection("plato").add(plato).await()
                limpiarCampos()
                mostrarListado()
            } catch (e: Exception) {
                mostrarError("Error al guardar: ${e.message}")
            }
        }
    }

    // Updates the current dish
    private fun actualizarPlato() {
        val plato = platoEditando ?: return

        val nombre = editNombre.text.toString().trim()
        val precio = editPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val categoria = editCategoria.text.toString().trim().uppercase()

        if (nombre.isEmpty() || categoria.isEmpty()) {
            mostrarError("Rellena todos los campos.")
            return
        }

        if (categoria !in categoriasValidas) {
            mostrarError("Categoría no válida.")
            return
        }

        lifecycleScope.launch {
            try {
                db.collection("plato").document(plato.id).update(
                    mapOf("nombre" to nombre, "precio" to precio, "categoria" to categoria)
                ).await()
                limpiarCampos()
                mostrarListado()
            } catch (e: Exception) {
                mostrarError("Error al actualizar: ${e.localizedMessage}")
            }
        }
    }

    // Deactivates a dish (soft delete)
    private fun eliminarPlato(plato: Plato) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmación")
            .setMessage("¿Seguro que quieres borrar este plato?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Borrar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("plato").document(plato.id).update("activo", false).await()
                        cargarPlatos()
                    } catch (e: Exception) {
                        mostrarError("Error al borrar: ${e.message}")
                    }
                }
            }
            .show()
    }

    // Loads all active dishes into the adapter
    private fun cargarPlatos() {
        lifecycleScope.launch {
            try {
                val result = db.collection("plato")
                    .whereEqualTo("activo", true)
                    .get().await()
                val platos = result.map { doc ->
                    Plato(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        categoria = doc.getString("categoria") ?: "",
                        activo = doc.getBoolean("activo") ?: true
                    )
                }
                (recyclerPlatosGlobal.adapter as? PlatoAdapter)?.actualizar(platos)
            } catch (e: Exception) {
                mostrarError("Error al cargar los platos.")
            }
        }
    }

    // Loads and displays dishes grouped by category
    private fun cargarPlatosAgrupados() {
        layoutListadoPlatos.removeAllViews()

        lifecycleScope.launch {
            try {
                val result = db.collection("plato")
                    .whereEqualTo("activo", true)
                    .get()
                    .await()

                val platos = result.map { doc ->
                    Plato(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        categoria = doc.getString("categoria") ?: "",
                        activo = true
                    )
                }

                val categorias = listOf("BEBIDAS", "ENTRANTES", "PRIMEROS", "SEGUNDOS", "POSTRES")

                for (categoria in categorias) {
                    val tituloCategoria = TextView(requireContext()).apply {
                        text = categoria.replaceFirstChar { it.uppercase() }
                        textSize = 20f
                        setPadding(0, 24, 0, 8)
                    }
                    layoutListadoPlatos.addView(tituloCategoria)

                    platos.filter { it.categoria.equals(categoria, ignoreCase = true) }
                        .sortedBy { it.nombre.lowercase() }
                        .forEach { plato ->
                            val card = layoutInflater.inflate(R.layout.item_plato_vista, layoutListadoPlatos, false)
                            card.findViewById<TextView>(R.id.tvNombre).text = plato.nombre.replaceFirstChar { it.uppercase() }
                            card.findViewById<TextView>(R.id.tvPrecio).text = "${plato.precio} €"
                            layoutListadoPlatos.addView(card)
                        }
                }
            } catch (e: Exception) {
                mostrarError("Error al cargar el menú.")
            }
        }
    }

    // Displays an error dialog
    private fun mostrarError(mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
}
