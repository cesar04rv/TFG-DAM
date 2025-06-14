package com.cesar.tfg

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

public class GestionNavGraphDirections private constructor() {
  private data class ActionGlobalDetalleComandaFragment(
    public val comandaId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.actionGlobalDetalleComandaFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("comandaId", this.comandaId)
        return result
      }
  }

  public companion object {
    public fun actionGlobalDetalleComandaFragment(comandaId: String): NavDirections =
        ActionGlobalDetalleComandaFragment(comandaId)
  }
}
