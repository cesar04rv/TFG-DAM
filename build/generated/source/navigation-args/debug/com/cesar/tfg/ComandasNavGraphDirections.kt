package com.cesar.tfg

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class ComandasNavGraphDirections private constructor() {
  public companion object {
    public fun actionComandasHoyFragmentToComandasInicioFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_comandasHoyFragment_to_comandasInicioFragment)

    public fun actionMisComandasFragmentToComandasInicioFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_misComandasFragment_to_comandasInicioFragment)

    public fun actionComandasInicioFragmentToQrCartaFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_comandasInicioFragment_to_qrCartaFragment)
  }
}
