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
import com.example.group2mts.data.HydrationRepository
import com.example.group2mts.data.SupabaseProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.providers.builtin.Email

class LoginActivity : AppCompatActivity() {

    private val client = SupabaseProvider.client
    private val auth = client.auth
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        client.handleDeeplinks(intent)

        lifecycleScope.launch {
            auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) goToMain()
            }
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnForgotPassword).setOnClickListener {
            // TODO: forgot password flow
            val emailInput = findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                toast("Enter your email above first")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    auth.resetPasswordForEmail(emailInput)
                    toast("Password reset email sent")
                } catch (e: Exception) {
                    toast(e.message ?: "Could not send reset email")
                }
            }
        }

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val emailInput = findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val passwordInput = findViewById<TextInputEditText>(R.id.etPassword).text.toString()

            // TODO: validate input and log in once the auth backend is decided
            when {
                !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches() -> toast("Enter a valid email")
                passwordInput.isEmpty() -> toast("Enter your password")
                else -> {
                    btnLogin.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            auth.signInWith(Email) {
                                email = emailInput
                                password = passwordInput
                            }
                            // navigation happens in the sessionStatus collector
                        } catch (e: Exception) {
                            toast(e.message ?: "Login failed")
                            btnLogin.isEnabled = true
                        }
                    }
                }
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

        findViewById<MaterialButton>(R.id.btnSignUp).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        client.handleDeeplinks(intent)
    }

    private fun goToMain() {
        if (navigated) return
        navigated = true
        lifecycleScope.launch {
            // A brand new account (for example Google sign up) has no gender yet, so it still
            // needs onboarding. If the profile cannot be read, go to the dashboard anyway.
            val needsOnboarding = try {
                HydrationRepository().getProfile().gender == null
            } catch (e: Exception) {
                false
            }
            val target = if (needsOnboarding) OnboardingActivity::class.java else MainActivity::class.java
            startActivity(Intent(this@LoginActivity, target))
            finishAffinity()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}