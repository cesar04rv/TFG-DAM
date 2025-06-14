package com.cesar.tfg.Model

// Data class representing a dish that has been added or modified in an order (comanda)
data class PlatoEditado(
    val plato: Plato,                 // The dish object itself
    var cantidadOriginal: Int,       // Original quantity of the dish in the order
    var cantidadRonda: Int = 0       // Additional quantity added later (e.g., another round)
)
