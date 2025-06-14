package com.cesar.tfg.Model

// Sealed class used to represent different types of items in the dish (plato) list.
// It allows combining headers (categories) and dish entries in the same RecyclerView.
sealed class PlatoItem {

    // Represents a header item for a dish category (e.g., "BEBIDAS", "ENTRANTES")
    data class Header(
        val categoria: String,         // Name of the category
        var expandido: Boolean = true  // Whether the category is currently expanded
    ) : PlatoItem()

    // Represents a regular dish item
    data class PlatoData(
        val plato: Plato               // The dish data
    ) : PlatoItem()
}
