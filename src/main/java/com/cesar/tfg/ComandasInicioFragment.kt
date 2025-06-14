package com.cesar.tfg

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cesar.tfg.View.Activity.LoginActivity
import com.cesar.tfg.View.Activity.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ComandasInicioFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comandas_inicio, container, false)

        val cardNuevaComanda = view.findViewById<CardView>(R.id.cardNuevaComanda)
        val cardMisComandas = view.findViewById<CardView>(R.id.cardMisComandas)
        val cardComandasHoy = view.findViewById<CardView>(R.id.cardComandasHoy)
        val btnVerQR: ImageButton = view.findViewById(R.id.btnVerQR)
        val btnCerrarSesion: Button = view.findViewById(R.id.btnCerrarSesion)
        val btnVolver: Button = view.findViewById(R.id.btnVolver)

        // Hide return button if the user is not a manager
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            Firebase.firestore.collection("trabajador").document(uid).get()
                .addOnSuccessListener { doc ->
                    val esJefe = doc.getBoolean("es_jefe") ?: false
                    if (!esJefe) {
                        btnVolver.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    // For safety, hide the button in case of failure too
                    btnVolver.visibility = View.GONE
                }
        } else {
            btnVolver.visibility = View.GONE
        }

        // Navigate to QR view of the menu
        btnVerQR.setOnClickListener {
            val action = ComandasInicioFragmentDirections.actionComandasInicioFragmentToQrCartaFragment()
            findNavController().navigate(action)
        }

        // Navigate to "Create New Order" screen
        cardNuevaComanda.setOnClickListener {
            findNavController().navigate(R.id.action_comandasInicioFragment_to_crearComandaFragment)
        }

        // Navigate to "My Orders" screen
        cardMisComandas.setOnClickListener {
            findNavController().navigate(R.id.action_comandasInicioFragment_to_misComandasFragment)
        }

        // Navigate to "Today's Orders" screen
        cardComandasHoy.setOnClickListener {
            findNavController().navigate(R.id.action_comandasInicioFragment_to_comandasHoyFragment)
        }

        // Logout process with confirmation dialog
        btnCerrarSesion.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmar cierre de sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aceptar") { _, _ ->
                    // Sign out from Firebase Auth
                    Firebase.auth.signOut()

                    // Clear login preferences
                    val prefs = requireContext().getSharedPreferences("loginPrefs", AppCompatActivity.MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    // Redirect to Login screen and clear backstack
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .show()
        }

        // Return to MainActivity (only visible if user is a manager)
        btnVolver.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
