package com.example.group2mts

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.SupabaseProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


class RegisterActivity : AppCompatActivity() {

    private val auth = SupabaseProvider.client.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)
        btnCreate.setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.etFullName).text.toString().trim()
            val emailInput = findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val passwordInput = findViewById<TextInputEditText>(R.id.etPassword).text.toString()

            // TODO: validate input and register once the auth backend is decided
            when {
                name.isEmpty() -> toast("Please enter your name")
                !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches() -> toast("Enter a valid email")
                passwordInput.length < 6 -> toast("Password must be at least 6 characters")
                else -> register(btnCreate, name, emailInput, passwordInput)
            }
        }

        findViewById<MaterialButton>(R.id.btnGoogle).setOnClickListener {
            // TODO: Google sign-in once the auth backend is decided
            lifecycleScope.launch {
                try {
                    auth.signInWith(Google)
                } catch (e: Exception) {
                    toast(e.message ?: "Google sign-in failed")
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnGoToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun register(button: MaterialButton, name: String, emailInput: String, passwordInput: String) {
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                auth.signUpWith(Email) {
                    email = emailInput
                    password = passwordInput
                    data = buildJsonObject { put("full_name", name) }
                }

                if (auth.currentSessionOrNull() != null) {
                    // Email confirmation is OFF: user is signed in
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                } else {
                    // Email confirmation is ON: user must verify first
                    toast("Check your email to confirm your account, then log in.")
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                toast(e.message ?: "Registration failed")
                button.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}