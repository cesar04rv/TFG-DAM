package com.cesar.tfg

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class EditarComandaFragmentArgs(
  public val comandaId: String,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("comandaId", this.comandaId)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("comandaId", this.comandaId)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): EditarComandaFragmentArgs {
      bundle.setClassLoader(EditarComandaFragmentArgs::class.java.classLoader)
      val __comandaId : String?
      if (bundle.containsKey("comandaId")) {
        __comandaId = bundle.getString("comandaId")
        if (__comandaId == null) {
          throw IllegalArgumentException("Argument \"comandaId\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"comandaId\" is missing and does not have an android:defaultValue")
      }
      return EditarComandaFragmentArgs(__comandaId)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): EditarComandaFragmentArgs {
      val __comandaId : String?
      if (savedStateHandle.contains("comandaId")) {
        __comandaId = savedStateHandle["comandaId"]
        if (__comandaId == null) {
          throw IllegalArgumentException("Argument \"comandaId\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"comandaId\" is missing and does not have an android:defaultValue")
      }
      return EditarComandaFragmentArgs(__comandaId)
    }
  }
}
