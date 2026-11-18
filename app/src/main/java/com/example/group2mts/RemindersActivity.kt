package com.example.group2mts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.HydrationRepository
import kotlinx.coroutines.launch

class RemindersActivity : AppCompatActivity() {

    private val repo = HydrationRepository()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            saveAndGo(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reminders)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                bars.left + dp(24), bars.top, bars.right + dp(24), bars.bottom + dp(24)
            )
            insets
        }

        findViewById<View>(R.id.btnSetReminder).setOnClickListener { askAndContinue() }
        findViewById<View>(R.id.btnMaybeLater).setOnClickListener { saveAndGo(false) }
        findViewById<View>(R.id.btnBack).setOnClickListener { saveAndGo(false) }
    }

    private fun askAndContinue() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            saveAndGo(true)
        }
    }

    private fun saveAndGo(enabled: Boolean) {
        findViewById<View>(R.id.btnSetReminder).isEnabled = false
        findViewById<View>(R.id.btnMaybeLater).isEnabled = false
        ReminderScheduler.setEnabled(this, enabled)
        lifecycleScope.launch {
            try {
                repo.updateProfile { set("reminder_enabled", enabled) }
            } catch (e: Exception) {
            }
            startActivity(Intent(this@RemindersActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
