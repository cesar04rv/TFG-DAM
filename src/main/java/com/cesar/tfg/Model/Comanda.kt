package com.cesar.tfg.Model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

// Data class representing a Comanda (order) document in Firestore
data class Comanda(
    val id: String = "",                          // Unique ID of the order (document ID)
    val numero: Long = 0,                         // Order number
    val fecha: Timestamp = Timestamp.now(),       // Timestamp of when the order was created
    val estado: String = "",                      // Order status (e.g., "ACTIVA", "FINALIZADA")
    val precio: Double = 0.0,                     // Total price of the order
    val idMesa: DocumentReference? = null,        // Reference to the table (mesa) document
    var numeroMesa: Int = 0,                      // Human-readable table number
    var nombreTrabajador: String = ""             // Name of the worker who created the order
)
