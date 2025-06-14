package com.cesar.tfg.utils

import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

// Extension function to set white status and navigation bars in a Fragment
fun Fragment.configurarBarrasBlancas() {
    val window = requireActivity().window
    window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.white)
    window.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.white)

    // Apply light mode for status and navigation bars if supported
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
    }
}

// Extension function to set white status and navigation bars in an Activity
fun AppCompatActivity.configurarBarrasBlancas() {
    window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
    window.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)

    // Apply light mode for status and navigation bars if supported
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
    }
}
