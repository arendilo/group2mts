package com.example.group2mts

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.HydrationRepository
import com.example.group2mts.data.SupabaseProvider
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val repo = HydrationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnOpenOnboarding).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            lifecycleScope.launch {
                runCatching { SupabaseProvider.client.auth.signOut() }
                startActivity(Intent(this@ProfileActivity, WelcomeActivity::class.java))
                finishAffinity()
            }
        }

        lifecycleScope.launch {
            try {
                val p = repo.getProfile()
                val email = SupabaseProvider.client.auth.currentUserOrNull()?.email.orEmpty()
                findViewById<TextView>(R.id.tvProfileName).text = p.fullName.ifBlank { "Gluggo User" }
                findViewById<TextView>(R.id.tvProfileDetails).text =
                    "$email\nWeight: ${p.weightKg} kg · Activity: ${p.activityLevel} · Unit: ml"
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, e.message ?: "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }
}