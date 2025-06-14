package com.cesar.tfg

import androidx.navigation.NavDirections

public class QrCartaFragmentDirections private constructor() {
  public companion object {
    public fun actionComandasHoyFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasHoyFragmentToComandasInicioFragment()

    public fun actionMisComandasFragmentToComandasInicioFragment(): NavDirections =
        ComandasNavGraphDirections.actionMisComandasFragmentToComandasInicioFragment()

    public fun actionComandasInicioFragmentToQrCartaFragment(): NavDirections =
        ComandasNavGraphDirections.actionComandasInicioFragmentToQrCartaFragment()
  }
}
