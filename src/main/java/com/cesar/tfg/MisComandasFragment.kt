package com.cesar.tfg

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.tfg.Model.Comanda
import com.cesar.tfg.Controller.MisComandasAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.util.Calendar

class MisComandasFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnAlternarEstado: Button
    private lateinit var btnVolver: Button

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val comandas = mutableListOf<Comanda>()
    private lateinit var adapter: MisComandasAdapter
    private var mostrarFinalizadas = false // Track toggle state

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_mis_comandas, container, false)

        // Bind views
        recycler = view.findViewById(R.id.recyclerMisComandas)
        btnAlternarEstado = view.findViewById(R.id.btnAlternarEstado)
        btnVolver = view.findViewById(R.id.btnVolver)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        // Toggle between finalized and active orders
        btnAlternarEstado.setOnClickListener {
            mostrarFinalizadas = !mostrarFinalizadas
            btnAlternarEstado.text = if (mostrarFinalizadas) "Activas" else "Finalizadas"
            cargarComandas()
        }

        // Return to start fragment
        btnVolver.setOnClickListener {
            val action = MisComandasFragmentDirections.actionMisComandasFragmentToComandasInicioFragment()
            findNavController().navigate(action)
        }

        btnAlternarEstado.text = "Finalizadas"
        cargarComandas()

        return view
    }

    // Loads the orders based on the current toggle state (active/finalized)
    private fun cargarComandas() {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val timestampHoy = Timestamp(hoy)
        val uid = auth.currentUser?.uid ?: return
        val estado = if (mostrarFinalizadas) "finalizada" else "abierta"

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("comanda")
                    .whereEqualTo("id_trabajador", db.collection("trabajador").document(uid))
                    .whereGreaterThan("fecha", timestampHoy)
                    .whereEqualTo("estado", estado)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get().await()

                comandas.clear()
                for (doc in snapshot) {
                    val idMesaRef = doc.getDocumentReference("id_mesa")
                    val mesaNumero = idMesaRef?.get()?.await()?.getLong("numero_mesa") ?: 0
                    val nombreTrabajador = doc.getString("nombre_trabajador") ?: ""

                    comandas.add(
                        Comanda(
                            id = doc.id,
                            numero = doc.getLong("numero_comanda") ?: 0,
                            fecha = doc.getTimestamp("fecha") ?: Timestamp.now(),
                            estado = doc.getString("estado") ?: "",
                            precio = doc.getDouble("precio") ?: 0.0,
                            idMesa = idMesaRef,
                            numeroMesa = mesaNumero.toInt(),
                            nombreTrabajador = nombreTrabajador
                        )
                    )
                }

                crearAdapter()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Creates and sets up the adapter with the proper configuration
    private fun crearAdapter() {
        adapter = MisComandasAdapter(
            comandas,
            onFinalizarClick = { confirmarCambioEstado(it) },
            onEditarClick = { editarComanda(it) },
            onCuentaClick = { pedirCorreoYEnviarCuenta(it) },
            mostrarBotones = !mostrarFinalizadas
        )
        recycler.adapter = adapter
    }

    // Shows a dialog to enter the client's email and sends the order summary via Firebase
    private fun pedirCorreoYEnviarCuenta(comanda: Comanda) {
        val input = EditText(requireContext()).apply {
            hint = "Correo del cliente"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Correo")
            .setMessage("Introduzca el email  del cliente:")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Enviar") { _, _ ->
                val correo = input.text.toString().trim()
                if (correo.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val resumen = generarResumen(comanda.id)
                            val data = hashMapOf(
                                "comandaId" to comanda.id,
                                "email" to correo,
                                "resumen" to resumen
                            )
                            Firebase.firestore.collection("send_email_requests")
                                .add(data)
                                .await()

                            Toast.makeText(requireContext(), "Sending email...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Failed to send: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }

    // Generates a formatted string summary of the order to send by email
    private suspend fun generarResumen(comandaId: String): String {
        val db = Firebase.firestore
        val comandaRef = db.collection("comanda").document(comandaId)
        val platosRef = db.collection("comanda_plato")
            .whereEqualTo("id_comanda", comandaRef)
            .get()
            .await()

        val mapa = mutableMapOf<String, Int>()

        for (doc in platosRef) {
            val platoRef = doc.getDocumentReference("id_plato") ?: continue
            val id = platoRef.id
            val cantidad = doc.getLong("cantidad")?.toInt() ?: 1
            mapa[id] = (mapa[id] ?: 0) + cantidad
        }

        val resumen = StringBuilder()
        var total = 0.0

        for ((id, cantidad) in mapa) {
            val platoDoc = db.collection("plato").document(id).get().await()
            val nombre = platoDoc.getString("nombre") ?: "Dish"
            val precio = platoDoc.getDouble("precio") ?: 0.0
            resumen.append("• $nombre x$cantidad — %.2f €\n".format(precio * cantidad))
            total += precio * cantidad
        }

        resumen.append("\nTOTAL: %.2f €".format(total))
        return resumen.toString()
    }

    // Confirms and toggles the order state between open/finalized
    private fun confirmarCambioEstado(comanda: Comanda) {
        val mensaje = if (mostrarFinalizadas) {
            "¿Quieres reactivar esta comanda?"
        } else {
            "¿Quieres finalizar esta comanda?"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Copnfirmación")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val nuevoEstado = if (mostrarFinalizadas) "abierta" else "finalizada"
                        db.collection("comanda").document(comanda.id)
                            .update("estado", nuevoEstado).await()
                        cargarComandas()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // Navigates to Edit Order screen only if the order is not finalized
    private fun editarComanda(comanda: Comanda) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("comanda").document(comanda.id).get().await()
                val estado = doc.getString("estado") ?: ""

                if (estado.lowercase() == "finalizada") {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Comanda finalizada")
                        .setMessage("No puedes editar una comanda que ya ha sido finalizada.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val action = MisComandasFragmentDirections
                        .actionMisComandasFragmentToEditarComandaFragment(comanda.id)
                    findNavController().navigate(action)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
