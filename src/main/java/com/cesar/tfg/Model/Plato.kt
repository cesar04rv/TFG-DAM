package com.cesar.tfg.Model

// Data class representing a dish (plato) in the menu
data class Plato(
    val id: String = "",              // Unique ID of the dish (usually the Firestore document ID)
    val nombre: String = "",          // Name of the dish
    val precio: Double = 0.0,         // Price of the dish
    val categoria: String = "",       // Category (e.g., Entrantes, Postres, Bebidas...)
    val activo: Boolean = true        // Indicates if the dish is active and visible in the app
)
