package com.cesar.tfg.View.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cesar.tfg.R

// Activity used to display and manage the "Comandas" screen (orders)
class ComandasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables drawing behind system bars (for edge-to-edge layout)
        enableEdgeToEdge()

        // Set the layout for this activity
        setContentView(R.layout.activity_comandas)

        // Apply system bar insets as padding to avoid overlapping UI elements
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
