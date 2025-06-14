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
import com.cesar.tfg.Controller.MisComandasAdapter
import com.cesar.tfg.Model.Comanda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class ComandasHoyFragment : Fragment() {

    // UI elements
    private lateinit var recycler: RecyclerView
    private lateinit var btnAlternarEstado: Button
    private lateinit var btnVolver: Button

    // Firebase and data
    private val db = Firebase.firestore
    private val comandas = mutableListOf<Comanda>()
    private lateinit var adapter: MisComandasAdapter
    private var mostrarFinalizadas = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_mis_comandas, container, false)

        // Initialize views
        recycler = view.findViewById(R.id.recyclerMisComandas)
        btnAlternarEstado = view.findViewById(R.id.btnAlternarEstado)
        btnVolver = view.findViewById(R.id.btnVolver)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        // Toggle between active and finalized orders
        btnAlternarEstado.setOnClickListener {
            mostrarFinalizadas = !mostrarFinalizadas
            btnAlternarEstado.text = if (mostrarFinalizadas) "Activas" else "Finalizadas"
            cargarComandasDeHoy() // Reload orders with updated state
        }

        // Navigate back to the start screen
        btnVolver.setOnClickListener {
            val action = ComandasHoyFragmentDirections.actionComandasHoyFragmentToComandasInicioFragment()
            findNavController().navigate(action)
        }

        // Set initial button text and load orders
        btnAlternarEstado.text = "Finalizadas"
        cargarComandasDeHoy()

        return view
    }

    // Fetch today's orders from Firestore and filter by status
    private fun cargarComandasDeHoy() {
        // Set time to start of the day (00:00:00)
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val timestampHoy = Timestamp(hoy)

        lifecycleScope.launch {
            try {
                val estado = if (mostrarFinalizadas) "finalizada" else "abierta"

                // Query orders from today and with the selected state
                val snapshot = db.collection("comanda")
                    .whereGreaterThan("fecha", timestampHoy)
                    .whereEqualTo("estado", estado)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get().await()

                comandas.clear()

                // Iterate through the documents and build the list of orders
                for (doc in snapshot.documents) {
                    val idMesaRef = doc.getDocumentReference("id_mesa")
                    val mesaNumero = idMesaRef?.get()?.await()?.getLong("numero_mesa") ?: 0
                    val nombreTrabajador = doc.getString("nombre_trabajador") ?: ""

                    // Create Comanda object from Firestore data
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

                // Load the adapter with the data
                crearAdapter()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Initialize and attach adapter to the RecyclerView
    private fun crearAdapter() {
        adapter = MisComandasAdapter(
            comandas,
            onFinalizarClick = { marcarComoFinalizada(it) },
            onEditarClick = { editarComanda(it) },
            onCuentaClick = { pedirCorreoYEnviarCuenta(it) },
            mostrarBotones = !mostrarFinalizadas // Hide buttons for finalized orders
        )
        recycler.adapter = adapter
    }

    // Ask for customer email and send the receipt
    private fun pedirCorreoYEnviarCuenta(comanda: Comanda) {
        val input = EditText(requireContext()).apply {
            hint = "Correo del cliente"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        // Show dialog to enter email
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Correo electrónico")
            .setMessage("Introduzca el email  del cliente:")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Enviar") { _, _ ->
                val correo = input.text.toString().trim()
                if (correo.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Generate summary of the order to be sent
                            val resumen = generarResumen(comanda.id)

                            val data = hashMapOf(
                                "comandaId" to comanda.id,
                                "email" to correo,
                                "resumen" to resumen
                            )

                            // Request backend to send the email via Firestore trigger
                            Firebase.firestore.collection("send_email_requests")
                                .add(data)
                                .await()

                            Toast.makeText(requireContext(), "Enviando la cuenta...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al enviar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }

    // Create order summary string with item details
    private suspend fun generarResumen(comandaId: String): String {
        val db = Firebase.firestore
        val comandaRef = db.collection("comanda").document(comandaId)

        // Fetch related dish documents from order
        val platosRef = db.collection("comanda_plato")
            .whereEqualTo("id_comanda", comandaRef)
            .get()
            .await()

        val mapa = mutableMapOf<String, Int>()

        // Build a map of dish ID to total quantity
        for (doc in platosRef) {
            val platoRef = doc.getDocumentReference("id_plato") ?: continue
            val id = platoRef.id
            val cantidad = doc.getLong("cantidad")?.toInt() ?: 1
            mapa[id] = (mapa[id] ?: 0) + cantidad
        }

        val resumen = StringBuilder()
        var total = 0.0

        // Loop through each dish to generate the summary line
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

    // Change order status to active or finalized
    private fun marcarComoFinalizada(comanda: Comanda) {
        val mensaje = if (mostrarFinalizadas) {
            "¿Quieres reactivar esta comanda?"
        } else {
            "¿Quieres finalizar esta comanda?"
        }

        // Show confirmation dialog before updating
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmación")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val nuevoEstado = if (mostrarFinalizadas) "abierta" else "finalizada"
                        db.collection("comanda").document(comanda.id)
                            .update("estado", nuevoEstado).await()
                        cargarComandasDeHoy() // Reload after update
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // Allow editing only if the order is not finalized
    private fun editarComanda(comanda: Comanda) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("comanda").document(comanda.id).get().await()
                val estado = doc.getString("estado") ?: ""

                if (estado.lowercase() == "finalizada") {
                    // Block editing for finalized orders
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Comanda finalizada")
                        .setMessage("No puedes editar una comanda que ya ha sido finalizada.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // Navigate to the edit screen
                    val action = ComandasHoyFragmentDirections
                        .actionComandasHoyFragmentToEditarComandaFragment(comanda.id)
                    findNavController().navigate(action)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
