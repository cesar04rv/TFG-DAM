package com.cesar.tfg.View.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.cesar.tfg.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.cesar.tfg.utils.configurarBarrasBlancas

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarBarrasBlancas()
        setContentView(R.layout.activity_main)

        // UI elements
        val btnCerrarSesion: Button = findViewById(R.id.btnCerrarSesion)
        val cardComandas = findViewById<CardView>(R.id.cardComandas)
        val cardGestion = findViewById<CardView>(R.id.cardGestion)

        // Navigate to ComandasActivity
        cardComandas.setOnClickListener {
            startActivity(Intent(this, ComandasActivity::class.java))
        }

        // Navigate to GestionActivity
        cardGestion.setOnClickListener {
            startActivity(Intent(this, GestionActivity::class.java))
        }

        // Log out logic
        btnCerrarSesion.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar cierre de sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aceptar") { _, _ ->
                    // Sign out from Firebase Authentication
                    Firebase.auth.signOut()

                    // Clear saved login preferences
                    val prefs = this.getSharedPreferences("loginPrefs", AppCompatActivity.MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    // Go back to LoginActivity and clear activity stack
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .show()
        }
    }
}
