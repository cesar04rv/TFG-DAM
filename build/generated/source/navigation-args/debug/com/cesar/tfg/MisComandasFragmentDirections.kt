package com.cesar.tfg

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

public class MisComandasFragmentDirections private constructor() {
  private data class ActionMisComandasFragmentToEditarComandaFragment(
    public val comandaId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.actionMisComandasFragmentToEditarComandaFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("comandaId", this.comandaId)
        return result
      }
  }

  public companion object {
    public fun actionMisComandasFragmentToEditarComandaFragment(comandaId: String): NavDirections =
        ActionMisComandasFragmentToEditarComandaFragment(comandaId)

    public fun actionComandasHoyFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasHoyFragmentToComandasInicioFragment()

    public fun actionMisComandasFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionMisComandasFragmentToComandasInicioFragment()

    public fun actionComandasInicioFragmentToQrCartaFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasInicioFragmentToQrCartaFragment()
  }
}
