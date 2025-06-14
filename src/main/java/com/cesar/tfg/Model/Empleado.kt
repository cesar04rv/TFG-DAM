package com.cesar.tfg.Model

// Data class representing an employee in the system
data class Empleado(
    val id: String = "",                   // Unique ID of the employee (usually from Firestore)
    val nombre: String = "",               // First name of the employee
    val email: String? = null,             // Optional email address of the employee
    val apellidos: String = "",            // Last name(s) of the employee
    val esJefe: Boolean = false,           // Indicates whether the employee is a manager (true = manager)
    val activo: Boolean = true             // Indicates if the employee is active in the system
) {
}
