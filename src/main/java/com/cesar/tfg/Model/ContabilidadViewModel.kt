package com.cesar.tfg.Model

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

// ViewModel used to store filters and state for the accounting section (Contabilidad)
class ContabilidadViewModel : ViewModel() {
    var fechaInicio: Timestamp? = null                       // Start date for filtering orders
    var fechaFin: Timestamp? = null                          // End date for filtering orders
    var nombreEmpleadoSeleccionado: String? = null           // Selected employee's name (for display)
    var referenciaEmpleadoSeleccionado: DocumentReference? = null // Firestore reference to selected employee
}
