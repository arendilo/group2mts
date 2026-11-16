package com.example.group2mts

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.DrinkLog
import com.example.group2mts.data.GoalCalculator
import com.example.group2mts.data.HydrationRepository
import com.example.group2mts.data.SupabaseProvider
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// One "sip" the user logged.
// consumedMl = volume yang diminum, creditedMl = volume yang dihitung ke target
data class DrinkRecord(
    val name: String,
    val consumedMl: Int,
    val creditedMl: Int,
    val time: LocalTime
)
class MainActivity : AppCompatActivity() {

    private val repo = HydrationRepository()
    private var dailyGoalMl = 2000
    private var selectedMl = 200
    private var records: List<DrinkRecord> = emptyList()

    private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private lateinit var chips: Map<MaterialButton, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Keep content out from under the status bar, and push the nav bar above the gesture bar.
        val navBar = findViewById<View>(R.id.navBar)
        val navBarBasePadding = navBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            navBar.updatePadding(bottom = navBarBasePadding + systemBars.bottom)
            insets
        }

        // Header date, e.g. "SUNDAY, NOVEMBER 15"
        findViewById<TextView>(R.id.tvDate).text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
            .uppercase()

        findViewById<MaterialButton>(R.id.btnPeriod).setOnClickListener {
            // TODO: period picker (Today / Week / Month)
        }

        // Chips: tapping one just remembers the amount and re-colors the row.
        chips = mapOf(
            findViewById<MaterialButton>(R.id.chip100) to 100,
            findViewById<MaterialButton>(R.id.chip200) to 200,
            findViewById<MaterialButton>(R.id.chip350) to 350,
            findViewById<MaterialButton>(R.id.chip500) to 500,
        )
        chips.forEach { (button, ml) ->
            button.setOnClickListener {
                selectedMl = ml
                updateChips()
            }
        }

        // The round "+" button logs a new drink using the selected amount.
        val fab = findViewById<View>(R.id.fabAdd)
        fab.setOnClickListener {
            fab.isEnabled = false
            lifecycleScope.launch {
                try {
                    repo.logDrink("Water", "Water", "#1FB6D5", selectedMl, 100)
                    reloadLogs()
                } catch (e: Exception) {
                    toast(e.message ?: "Failed to save drink")
                } finally {
                    fab.isEnabled = true
                }
            }
        }

        //history
        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(android.content.Intent(this, HistoryActivity::class.java))
        }

        updateChips()
        render()
        loadToday()

    }

    // Cek sesi, pastikan target hari ini ada, lalu ambil log hari ini
    private fun loadToday() {
        lifecycleScope.launch {
            try {
                val auth = SupabaseProvider.client.auth
                auth.awaitInitialization()
                if (auth.currentSessionOrNull() == null) {
                    startActivity(Intent(this@MainActivity, WelcomeActivity::class.java))
                    finish()
                    return@launch
                }

                val today = LocalDate.now().toString()
                var target = repo.getTarget(today)
                if (target == null) {
                    target = GoalCalculator.build(repo.getProfile(), today)
                    repo.upsertTodayTarget(target)
                }
                dailyGoalMl = target.totalGoalMl

                reloadLogs()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to load data")
            }
        }
    }

    private suspend fun reloadLogs() {
        records = repo.getLogsForDate(LocalDate.now().toString()).map { it.toRecord() }
        render()
    }

    private fun DrinkLog.toRecord() = DrinkRecord(
        name = drinkName,
        consumedMl = consumedMl,
        creditedMl = creditedMl,
        time = loggedAt?.let {
            OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalTime()
        } ?: LocalTime.now()
    )

    // Selected chip = blue (Primary look), others = white (Secondary.Dark look).
    private fun updateChips() {
        chips.forEach { (button, ml) ->
            val color = if (ml == selectedMl) R.color.primary else R.color.surface
            button.backgroundTintList = ColorStateList.valueOf(getColor(color))
        }
    }

    // Re-draws everything that depends on `records`. Call it after any change.
    private fun render() {
        val total = records.sumOf { it.creditedMl }
        val percent = if (dailyGoalMl > 0) (total * 100 / dailyGoalMl).coerceAtMost(100) else 0

        findViewById<TextView>(R.id.tvTotal).text = String.format(Locale.US, "%,d ml", total)
        findViewById<TextView>(R.id.tvGoal).text =
            String.format(Locale.US, "of %,d ml daily goal", dailyGoalMl)
        findViewById<TextView>(R.id.tvPercent).text = "$percent%"
        findViewById<TextView>(R.id.tvSips).text = "${records.size} sips today"

        val list = findViewById<LinearLayout>(R.id.listRecords)
        list.removeAllViews()
        records.forEachIndexed { index, record ->
            val row = layoutInflater.inflate(R.layout.item_drink_record, list, false)
            row.findViewById<View>(R.id.divider).isVisible = index > 0
            row.findViewById<TextView>(R.id.tvRecordName).text = record.name
            row.findViewById<TextView>(R.id.tvRecordTime).text = record.time.format(timeFormat)
            row.findViewById<TextView>(R.id.tvRecordAmount).text =
                String.format(Locale.US, "%,d ml", record.consumedMl)
            list.addView(row)
        }
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

}
