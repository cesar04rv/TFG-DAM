package com.cesar.tfg.View.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cesar.tfg.R
import com.cesar.tfg.utils.configurarBarrasBlancas
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private val auth = Firebase.auth

    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var checkRemember: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarBarrasBlancas()
        setContentView(R.layout.activity_login)

        // Initialize UI elements
        loginEmail = findViewById(R.id.loginEmail)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginBtn)
        registerButton = findViewById(R.id.registerBtn)
        checkRemember = findViewById(R.id.checkRemember)

        // Load saved credentials if "remember me" was previously checked
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("remember", false)) {
            loginEmail.setText(prefs.getString("email", ""))
            loginPassword.setText(prefs.getString("password", ""))
            checkRemember.isChecked = true
        }

        // Fill fields from intent extras (optional)
        intent.getStringExtra("email")?.let { loginEmail.setText(it) }
        intent.getStringExtra("password")?.let { loginPassword.setText(it) }

        // Login button click listener
        loginButton.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString()

            when {
                email.isEmpty() || password.isEmpty() -> showToast("Completa todos los campos")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showToast("Correo inválido")
                else -> loginUsuario(email, password)
            }
        }

        // Navigate to register screen
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        // Forgot password text click listener
        val tvOlvidarPassword = findViewById<TextView>(R.id.tvOlvidarPassword)
        tvOlvidarPassword.setOnClickListener {
            val input = EditText(this).apply {
                hint = "Correo electrónico"
                inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }

            // Show password reset dialog
            MaterialAlertDialogBuilder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Te enviaremos un correo para restablecer tu contraseña.")
                .setView(input)
                .setPositiveButton("Enviar") { _, _ ->
                    val correo = input.text.toString().trim()
                    if (correo.isNotEmpty()) {
                        Firebase.auth.sendPasswordResetEmail(correo)
                            .addOnSuccessListener {
                                showToast("Correo enviado correctamente")
                            }
                            .addOnFailureListener {
                                showToast("Error: ${it.message}")
                            }
                    } else {
                        showToast("Introduce un correo válido")
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // Function to perform user login
    private fun loginUsuario(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Sign in with Firebase Auth
                auth.signInWithEmailAndPassword(email, password).await()
                val uid = auth.currentUser?.uid ?: return@launch

                // Retrieve corresponding Firestore document
                val documento = Firebase.firestore.collection("trabajador").document(uid).get().await()
                if (!documento.exists()) {
                    auth.signOut()
                    showToast("Usuario no encontrado en base de datos")
                    return@launch
                }

                // Check if account is active
                val activo = documento.getBoolean("activo") ?: false
                if (!activo) {
                    auth.signOut()
                    showToast("Tu cuenta ha sido desactivada")
                    return@launch
                }

                // Save credentials if checkbox is checked
                val editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit()
                if (checkRemember.isChecked) {
                    editor.putString("email", email)
                    editor.putString("password", password)
                    editor.putBoolean("remember", true)
                } else {
                    editor.clear()
                }
                editor.apply()

                // Redirect based on user role (boss or regular)
                val esJefe = documento.getBoolean("es_jefe") ?: false
                val intent = if (esJefe) Intent(this@LoginActivity, MainActivity::class.java)
                else Intent(this@LoginActivity, ComandasActivity::class.java)

                startActivity(intent)
                finish()

            } catch (e: Exception) {
                showToast("Error al iniciar sesión: Datos incorrectos")
            }
        }
    }

    // Utility function to show a toast message
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
