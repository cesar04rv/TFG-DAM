package com.cesar.tfg

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.button.MaterialButton
import com.cesar.tfg.utils.configurarBarrasBlancas

class GestionContabilidadFragment : Fragment() {

    // Keeps track of the last selected sub-fragment type
    var ultimoFragmentoContabilidad = "hoy"

    // Controls the ordering direction for the 'today' view
    private var ordenAscendente = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_gestion_contabilidad, container, false)

        // Configures system bars with white background
        configurarBarrasBlancas()

        // Initialize buttons for each accounting section
        val btnHoy = view.findViewById<MaterialButton>(R.id.btnHoy)
        val btnRango = view.findViewById<MaterialButton>(R.id.btnRango)
        val btnHistorico = view.findViewById<MaterialButton>(R.id.btnHistorico)

        // Show the last visited fragment on view creation
        mostrarFragmento(ultimoFragmentoContabilidad)

        // Navigate to today's accounting fragment
        btnHoy.setOnClickListener {
            mostrarFragmento("hoy")
        }

        // Navigate to date range accounting fragment
        btnRango.setOnClickListener {
            mostrarFragmento("fechas")
        }

        // Navigate to full historical accounting fragment
        btnHistorico.setOnClickListener {
            mostrarFragmento("historico")
        }

        return view
    }

    /**
     * Replaces the current sub-fragment in the container based on the type specified.
     * Supported types: "hoy", "fechas", "historico"
     */
    private fun mostrarFragmento(tipo: String) {
        // Store selected type for potential re-use or restoration
        ultimoFragmentoContabilidad = tipo

        // Instantiate appropriate fragment based on type
        val fragment = when (tipo) {
            "fechas" -> {
                ContabilidadPorFechasFragment()
            }
            "historico" -> {
                ContabilidadHistoricoFragment()
            }
            else -> {
                // For "hoy", pass ordering preference via arguments
                ContabilidadHoyFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("ordenAscendente", ordenAscendente)
                    }
                }
            }
        }

        // Replace the current fragment inside the container
        childFragmentManager.commit {
            replace(R.id.contenedorContabilidad, fragment)
        }
    }
}
