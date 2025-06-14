package com.cesar.tfg

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

public class ComandasHoyFragmentDirections private constructor() {
  private data class ActionComandasHoyFragmentToEditarComandaFragment(
    public val comandaId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_comandasHoyFragment_to_editarComandaFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("comandaId", this.comandaId)
        return result
      }
  }

  public companion object {
    public fun actionComandasHoyFragmentToEditarComandaFragment(comandaId: String): NavDirections =
        ActionComandasHoyFragmentToEditarComandaFragment(comandaId)

    public fun actionComandasHoyFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasHoyFragmentToComandasInicioFragment()

    public fun actionMisComandasFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionMisComandasFragmentToComandasInicioFragment()

    public fun actionComandasInicioFragmentToQrCartaFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasInicioFragmentToQrCartaFragment()
  }
}
