package com.cesar.tfg.View.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cesar.tfg.PlatoFragment
import com.cesar.tfg.R

class PlatoHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plato_host)

        // Load PlatoFragment into the fragment container when the activity starts
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, PlatoFragment())
            .commit()
    }
}
