package com.cesar.tfg.View.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cesar.tfg.R
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private val auth = Firebase.auth

    // Declare UI elements
    private lateinit var registerEmail: EditText
    private lateinit var registerPassword: EditText
    private lateinit var registerRepeatPassword: EditText
    private lateinit var registerName: EditText
    private lateinit var registerSurname: EditText
    private lateinit var registerButton: Button
    private lateinit var registerGoLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize UI elements
        registerEmail = findViewById(R.id.registerEmail)
        registerPassword = findViewById(R.id.registerPassword)
        registerRepeatPassword = findViewById(R.id.registerRepeatPassword)
        registerName = findViewById(R.id.registerName)
        registerSurname = findViewById(R.id.registerApellido)
        registerButton = findViewById(R.id.RegisterButton)
        registerGoLoginButton = findViewById(R.id.RegisterGoLoginButton)

        // Handle registration button click
        registerButton.setOnClickListener {
            val email = registerEmail.text.toString().trim()
            val password = registerPassword.text.toString()
            val repeatPassword = registerRepeatPassword.text.toString()
            val name = registerName.text.toString().trim()
            val surname = registerSurname.text.toString().trim()

            // Validate input fields
            when {
                email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty() || name.isEmpty() || surname.isEmpty() -> {
                    showToast("Completa todos los campos")
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast("Correo inválido")
                }
                password != repeatPassword -> {
                    showToast("Las contraseñas no coinciden")
                }
                else -> {
                    crearUsuario(email, password, name, surname)
                }
            }
        }

        // Go back to login screen
        registerGoLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Create a new user in Firebase Auth and Firestore
    private fun crearUsuario(email: String, password: String, nombre: String, apellidos: String) {
        lifecycleScope.launch {
            try {
                // Create user with email and password
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("No se pudo obtener el UID")

                // Prepare user data for Firestore
                val user = hashMapOf(
                    "nombre" to nombre,
                    "apellidos" to apellidos,
                    "email" to email,
                    "es_jefe" to false,
                    "activo" to true
                )

                // Save user data in Firestore
                Firebase.firestore.collection("trabajador").document(uid).set(user).await()

                showToast("Registro exitoso")

                // Redirect to login screen with email and password pre-filled
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("password", password)
                }
                startActivity(intent)
                finish()

            } catch (e: FirebaseAuthUserCollisionException) {
                showToast("Ya existe un usuario con este correo")
            } catch (e: Exception) {
                showToast("Error al registrar: ${e.localizedMessage}")
            }
        }
    }

    // Show a toast message
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
