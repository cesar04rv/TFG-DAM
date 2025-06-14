package com.cesar.tfg.View.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.cesar.tfg.R
import com.google.android.material.bottomnavigation.BottomNavigationView

// Activity for managing different administrative sections (e.g., dishes, employees, accounting)
class GestionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion)

        // Get the NavHostFragment used for navigation between fragments
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.gestion_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.gestionBottomNav)

        // Set the default selected item to "nav_platos"
        bottomNav.selectedItemId = R.id.nav_platos

        // Handle bottom navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_volver_main) {
                // If the "Back to Main" item is selected, go back to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            } else {
                // Otherwise, navigate to the corresponding fragment
                navController.navigate(item.itemId)
                true
            }
        }
    }
}
