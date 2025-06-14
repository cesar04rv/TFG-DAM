package com.cesar.tfg

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Controller.EmpleadoAdapter
import com.cesar.tfg.Model.Empleado
import com.cesar.tfg.utils.configurarBarrasBlancas
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GestionEmpleadosFragment : Fragment() {

    // UI components
    private lateinit var nombreInput: EditText
    private lateinit var apellidoInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var checkJefe: CheckBox

    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnCrear: Button
    private lateinit var btnEditar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnVolver: Button

    private lateinit var seccionCrear: View
    private lateinit var seccionEditar: View
    private lateinit var seccionEliminar: View
    private lateinit var seccionListado: LinearLayout

    private lateinit var recyclerEditar: RecyclerView
    private lateinit var recyclerEliminar: RecyclerView

    // Firebase references
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Local state
    private val listaEmpleados = mutableListOf<Empleado>()
    private var empleadoEditando: Empleado? = null

    // Adapters
    private lateinit var adapterEditar: EmpleadoAdapter
    private lateinit var adapterEliminar: EmpleadoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        configurarBarrasBlancas()
        val view = inflater.inflate(R.layout.fragment_empleados, container, false)
        inicializarVista(view)
        mostrarSeccionListado()
        return view
    }

    // Initializes the UI and sets up listeners
    private fun inicializarVista(view: View) {
        // Bind inputs
        nombreInput = view.findViewById(R.id.editNombreEmpleado)
        apellidoInput = view.findViewById(R.id.editApellidoEmpleado)
        emailInput = view.findViewById(R.id.editEmailEmpleado)
        passwordInput = view.findViewById(R.id.editPasswordEmpleado)
        checkJefe = view.findViewById(R.id.checkEsJefe)

        // Bind buttons
        btnGuardar = view.findViewById(R.id.btnGuardarEmpleado)
        btnActualizar = view.findViewById(R.id.btnActualizarEmpleado)
        btnCrear = view.findViewById(R.id.btnCrearEmpleado)
        btnEditar = view.findViewById(R.id.btnEditarEmpleado)
        btnEliminar = view.findViewById(R.id.btnEliminarEmpleado)
        btnVolver = view.findViewById(R.id.btnVolverEmpleado)

        // Bind sections
        seccionCrear = view.findViewById(R.id.layoutCrearEmpleado)
        seccionEditar = view.findViewById(R.id.layoutEditarEmpleado)
        seccionEliminar = view.findViewById(R.id.layoutEliminarEmpleado)
        seccionListado = view.findViewById(R.id.layoutListadoEmpleados)

        // Setup RecyclerViews
        recyclerEditar = view.findViewById(R.id.recyclerEditarEmpleado)
        recyclerEliminar = view.findViewById(R.id.recyclerEliminarEmpleado)

        recyclerEditar.layoutManager = LinearLayoutManager(requireContext())
        recyclerEliminar.layoutManager = LinearLayoutManager(requireContext())

        adapterEditar = EmpleadoAdapter(listaEmpleados, EmpleadoAdapter.Modo.EDITAR) { cargarEmpleadoParaEditar(it) }
        adapterEliminar = EmpleadoAdapter(listaEmpleados, EmpleadoAdapter.Modo.ELIMINAR) { confirmarEliminacion(it) }

        recyclerEditar.adapter = adapterEditar
        recyclerEliminar.adapter = adapterEliminar

        // Click listeners
        btnCrear.setOnClickListener { mostrarFormulario(false) }
        btnEditar.setOnClickListener { mostrarSeccionEditar() }
        btnEliminar.setOnClickListener { mostrarSeccionEliminar() }
        btnGuardar.setOnClickListener { crearEmpleado() }
        btnActualizar.setOnClickListener { actualizarEmpleado() }

        // Handle back navigation depending on current visible section
        btnVolver.setOnClickListener {
            when {
                seccionCrear.visibility == View.VISIBLE -> {
                    if (btnActualizar.visibility == View.VISIBLE) mostrarSeccionEditar()
                    else mostrarSeccionListado()
                }
                seccionEditar.visibility == View.VISIBLE -> mostrarSeccionListado()
                seccionEliminar.visibility == View.VISIBLE -> mostrarSeccionListado()
            }
        }
    }

    // Shows a dialog with message
    private fun mostrarMensaje(titulo: String, mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun mostrarError(msg: String) = mostrarMensaje("Error", msg)
    private fun mostrarInfo(msg: String) = mostrarMensaje("Infornación", msg)

    // Shows create or edit form
    private fun mostrarFormulario(esEdicion: Boolean) {
        ocultarTodasLasSecciones()
        seccionCrear.visibility = View.VISIBLE
        btnVolver.visibility = View.VISIBLE

        if (esEdicion) {
            btnGuardar.visibility = View.GONE
            btnActualizar.visibility = View.VISIBLE
            emailInput.isEnabled = false
            passwordInput.visibility = View.GONE
        } else {
            btnGuardar.visibility = View.VISIBLE
            btnActualizar.visibility = View.GONE
            emailInput.isEnabled = true
            passwordInput.visibility = View.VISIBLE
            limpiarFormulario()
        }

        nombreInput.requestFocus()
        nombreInput.post {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(nombreInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    // Hides all content sections
    private fun ocultarTodasLasSecciones() {
        seccionCrear.visibility = View.GONE
        seccionEditar.visibility = View.GONE
        seccionEliminar.visibility = View.GONE
        seccionListado.visibility = View.GONE
    }

    private fun mostrarSeccionListado() {
        ocultarTodasLasSecciones()
        seccionListado.visibility = View.VISIBLE
        btnVolver.visibility = View.GONE
        cargarVistaEmpleados()
    }

    private fun mostrarSeccionEditar() {
        mostrarSeccion(seccionEditar)
        btnVolver.visibility = View.VISIBLE
        cargarListaEmpleados()
    }

    private fun mostrarSeccionEliminar() {
        mostrarSeccion(seccionEliminar)
        btnVolver.visibility = View.VISIBLE
        cargarListaEmpleados()
    }

    private fun mostrarSeccion(seccion: View) {
        ocultarTodasLasSecciones()
        seccion.visibility = View.VISIBLE
    }

    // Creates a new employee or reactivates an existing one
    private fun crearEmpleado() {
        val nombre = nombreInput.text.toString().trim()
        val apellido = apellidoInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val esJefe = checkJefe.isChecked

        if (nombre.isBlank() || apellido.isBlank() || email.isBlank() || password.isBlank()) {
            mostrarError("Por favor rellena todos los campos.")
            return
        }

        lifecycleScope.launch {
            try {
                val query = db.collection("trabajador")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (query.documents.isNotEmpty()) {
                    val doc = query.documents.first()
                    val idExistente = doc.id

                    db.collection("trabajador").document(idExistente).update(
                        mapOf(
                            "nombre" to nombre,
                            "apellidos" to apellido,
                            "es_jefe" to esJefe,
                            "activo" to true
                        )
                    ).await()

                    mostrarInfo("Empleado reactivado.")
                    limpiarFormulario()
                    mostrarSeccionListado()
                    cargarListaEmpleados()

                } else {
                    val datosEmpleado = hashMapOf(
                        "nombre" to nombre,
                        "apellidos" to apellido,
                        "email" to email,
                        "password" to password,
                        "esJefe" to esJefe
                    )

                    Firebase.functions
                        .getHttpsCallable("crearEmpleado")
                        .call(datosEmpleado)
                        .addOnSuccessListener { result ->
                            val mensaje = result.data as? Map<*, *>
                            when (mensaje?.get("mensaje")) {
                                "Empleado creado correctamente." -> {
                                    mostrarInfo("Empleado creado correctamente.")
                                    limpiarFormulario()
                                    mostrarSeccionListado()
                                    cargarListaEmpleados()
                                }
                                "Empleado reactivado correctamente." -> {
                                    mostrarInfo("Empleado reactivado.")
                                    limpiarFormulario()
                                    mostrarSeccionListado()
                                    cargarListaEmpleados()
                                }
                                "Usuario ya existente." -> {
                                    mostrarError("Ya hay un empleado con ese correo electrónico.")
                                }
                                else -> {
                                    mostrarError("Error")
                                }
                            }
                        }
                        .addOnFailureListener {
                            mostrarError("Error al crear empleado: ${it.message}")
                        }
                }
            } catch (e: Exception) {
                mostrarError("Error al comprobar empleado: ${e.localizedMessage}")
            }
        }
    }

    // Updates employee fields
    private fun actualizarEmpleado() {
        val empleado = empleadoEditando ?: return
        val nombre = nombreInput.text.toString().trim()
        val apellido = apellidoInput.text.toString().trim()
        val esJefe = checkJefe.isChecked

        if (nombre.isBlank() || apellido.isBlank()) {
            mostrarError("Por favor rellena todos los campos.")
            return
        }

        lifecycleScope.launch {
            try {
                db.collection("trabajador").document(empleado.id).update(
                    mapOf(
                        "nombre" to nombre,
                        "apellidos" to apellido,
                        "es_jefe" to esJefe
                    )
                ).await()
                mostrarInfo("Trabajador actualizado.")
                limpiarFormulario()
                mostrarSeccionListado()
                cargarListaEmpleados()
            } catch (e: Exception) {
                mostrarError("Error al actualizar: ${e.localizedMessage}")
            }
        }
    }

    // Asks for confirmation before disabling the employee
    private fun confirmarEliminacion(empleado: Empleado) {
        if (empleado.email == auth.currentUser?.email) {
            mostrarError("No puedes borrar tu propia cuenta.")
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Borrar empleado")
            .setMessage("¿Seguro que quieres borrar el empleado?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("trabajador").document(empleado.id).update("activo", false).await()
                        mostrarInfo("Trabajador borrado.")
                        mostrarSeccionListado()
                        cargarListaEmpleados()
                    } catch (e: Exception) {
                        mostrarError("Error al borrar: ${e.localizedMessage}")
                    }
                }
            }
            .show()
    }

    // Prepares employee for editing
    private fun cargarEmpleadoParaEditar(empleado: Empleado) {
        empleadoEditando = empleado
        nombreInput.setText(empleado.nombre)
        apellidoInput.setText(empleado.apellidos)
        emailInput.setText(empleado.email)
        checkJefe.isChecked = empleado.esJefe

        mostrarFormulario(true)
    }

    // Loads employee list into adapters
    private fun cargarListaEmpleados() {
        lifecycleScope.launch {
            try {
                val resultado = db.collection("trabajador").whereEqualTo("activo", true).get().await()
                listaEmpleados.clear()
                for (doc in resultado) {
                    listaEmpleados.add(
                        Empleado(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            apellidos = doc.getString("apellidos") ?: "",
                            email = doc.getString("email") ?: "",
                            esJefe = doc.getBoolean("es_jefe") ?: false,
                            activo = doc.getBoolean("activo") ?: true
                        )
                    )
                }
                adapterEditar.actualizar(listaEmpleados)
                adapterEliminar.actualizar(listaEmpleados)
            } catch (e: Exception) {
                mostrarError("Error al cargar empleados.")
            }
        }
    }

    // Displays employee cards
    private fun cargarVistaEmpleados() {
        seccionListado.removeAllViews()

        lifecycleScope.launch {
            try {
                val resultado = db.collection("trabajador").whereEqualTo("activo", true).get().await()
                for (doc in resultado) {
                    val empleado = Empleado(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        esJefe = doc.getBoolean("es_jefe") ?: false,
                        activo = doc.getBoolean("activo") ?: true
                    )

                    val card = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_empleado_vista, seccionListado, false)

                    card.findViewById<TextView>(R.id.tvNombreCompleto).text =
                        "${empleado.nombre.replaceFirstChar { it.uppercase() }} ${empleado.apellidos.replaceFirstChar { it.uppercase() }}"
                    card.findViewById<TextView>(R.id.tvEmailEmpleado).text = empleado.email
                    if (empleado.esJefe) {
                        card.findViewById<TextView>(R.id.tvEsJefe).visibility = View.VISIBLE
                    }

                    seccionListado.addView(card)
                }
            } catch (e: Exception) {
                mostrarError("Error al cargar empleados.")
            }
        }
    }

    // Clears the input form
    private fun limpiarFormulario() {
        nombreInput.text.clear()
        apellidoInput.text.clear()
        emailInput.text.clear()
        passwordInput.text.clear()
        checkJefe.isChecked = false
        empleadoEditando = null
    }
}
