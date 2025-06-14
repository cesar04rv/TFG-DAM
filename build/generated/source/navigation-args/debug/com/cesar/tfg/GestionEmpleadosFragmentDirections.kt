package com.cesar.tfg

import androidx.navigation.NavDirections
import kotlin.String

public class GestionEmpleadosFragmentDirections private constructor() {
  public companion object {
    public fun actionGlobalDetalleComandaFragment(comandaId: String): NavDirections =
        GestionNavGraphDirections.actionGlobalDetalleComandaFragment(comandaId)
  }
}
