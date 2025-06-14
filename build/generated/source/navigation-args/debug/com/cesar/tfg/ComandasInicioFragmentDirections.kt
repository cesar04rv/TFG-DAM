package com.cesar.tfg

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class ComandasInicioFragmentDirections private constructor() {
  public companion object {
    public fun actionComandasInicioFragmentToCrearComandaFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_comandasInicioFragment_to_crearComandaFragment)

    public fun actionComandasInicioFragmentToMisComandasFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_comandasInicioFragment_to_misComandasFragment)

    public fun actionComandasInicioFragmentToComandasHoyFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_comandasInicioFragment_to_comandasHoyFragment)

    public fun actionComandasHoyFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasHoyFragmentToComandasInicioFragment()

    public fun actionMisComandasFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionMisComandasFragmentToComandasInicioFragment()

    public fun actionComandasInicioFragmentToQrCartaFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasInicioFragmentToQrCartaFragment()
  }
}
