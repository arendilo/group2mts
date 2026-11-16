package com.example.group2mts

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.DailyTarget
import com.example.group2mts.data.DrinkLog
import com.example.group2mts.data.GoalCalculator
import com.example.group2mts.data.HydrationRepository
import com.example.group2mts.data.Profile
import com.example.group2mts.data.SupabaseProvider
import com.example.group2mts.data.WeatherService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// One "sip" the user logged.
// consumedMl = volume yang diminum, creditedMl = volume yang dihitung ke target
data class DrinkRecord(
    val name: String,
    val colorHex: String,
    val consumedMl: Int,
    val creditedMl: Int,
    val time: LocalTime
)
class MainActivity : AppCompatActivity() {

    private val repo = HydrationRepository()
    private var dailyGoalMl = 2000
    private var records: List<DrinkRecord> = emptyList()
    private var isFirstResume = true

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

        // Period button menu
        val btnPeriod = findViewById<MaterialButton>(R.id.btnPeriod)
        btnPeriod.setOnClickListener { view ->
            PopupMenu(this, view).apply {
                menu.add(0, 1, 0, "Today")
                menu.add(0, 2, 1, "Week & Month History")
                setOnMenuItemClickListener { item ->
                    if (item.itemId == 2) {
                        startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
                    }
                    true
                }
                show()
            }
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
                quickLogWater(button, ml)
            }
        }

        // The round "+" button logs a new drink using the selected amount.
//        val fab = findViewById<View>(R.id.fabAdd)
//        fab.setOnClickListener {
//            fab.isEnabled = false
//            lifecycleScope.launch {
//                try {
//                    repo.logDrink("Water", "Water", "#1FB6D5", selectedMl, 100)
//                    reloadLogs()
//                } catch (e: Exception) {
//                    toast(e.message ?: "Failed to save drink")
//                } finally {
//                    fab.isEnabled = true
//                }
//            }
//        }
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, LogDrinkActivity::class.java))
        }

        // Bottom Navigation
        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.navGoals).setOnClickListener {
            startActivity(Intent(this, DrinkShelfActivity::class.java))
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        render()
        loadToday()

    }

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        // Refresh today's logs after returning from LogDrinkActivity
        lifecycleScope.launch {
            runCatching { reloadLogs() }
        }
    }

    private fun quickLogWater(clickedButton: MaterialButton, ml: Int) {
        setChipsEnabled(false)
        clickedButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))

        lifecycleScope.launch {
            try {
                repo.logDrink(
                    name = "Water",
                    category = "Water",
                    colorHex = "#1FB6D5",
                    consumedMl = ml,
                    factorPct = 100
                )
                reloadLogs()
                toast("+$ml ml Water logged!")
            } catch (e: Exception) {
                toast(e.message ?: "Failed to save drink")
            } finally {
                clickedButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface))
                setChipsEnabled(true)
            }
        }
    }

    private fun setChipsEnabled(enabled: Boolean) {
        chips.keys.forEach { it.isEnabled = enabled }
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

                // 1. Load Profile & update greeting dynamically
                val profile = repo.getProfile()
                val displayName = profile.fullName.trim().ifEmpty { "Hydration Buddy" }
                findViewById<TextView>(R.id.tvGreeting).text = "Hey, $displayName!"

                // 2. Fetch live external Weather API (Open-Meteo) & sync daily_hydration_targets
                val today = LocalDate.now().toString()
                val existingTarget = repo.getTarget(today)
                val envType = existingTarget?.environmentType ?: "outdoor"

                val weatherResult = runCatching { WeatherService.fetchCurrentWeather() }
                val target: DailyTarget = if (weatherResult.isSuccess) {
                    val w = weatherResult.getOrThrow()
                    val calculated = GoalCalculator.build(
                        p = profile,
                        date = today,
                        environmentType = envType,
                        tempC = w.tempC,
                        humidityPct = w.humidityPct,
                        condition = w.condition
                    )
                    repo.upsertTodayTarget(calculated)
                    calculated
                } else {
                    existingTarget ?: GoalCalculator.build(profile, today, envType).also {
                        repo.upsertTodayTarget(it)
                    }
                }

                dailyGoalMl = target.totalGoalMl
                renderWeatherCard(profile, target)

                // 3. Load today's drink logs
                reloadLogs()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to load data")
            }
        }
    }

    private fun renderWeatherCard(profile: Profile, target: DailyTarget) {
        val tvTitle = findViewById<TextView>(R.id.tvWeatherTempCondition)
        val tvAdvice = findViewById<TextView>(R.id.tvWeatherAdvice)

        val tempText = target.weatherTempC?.let { "${it.roundToInt()}°C" } ?: "--°C"
        val condText = target.weatherCondition ?: "Outdoor"
        tvTitle.text = "$tempText $condText"

        tvAdvice.text = when {
            !profile.autoRecalculate ->
                "Custom goal active · +0 ml weather adjustment"
            target.environmentAddonMl > 0 ->
                "Hot weather! +${target.environmentAddonMl} ml recommended today"
            else ->
                "Mild weather · +0 ml extra needed today"
        }
    }

    private suspend fun reloadLogs() {
        records = repo.getLogsForDate(LocalDate.now().toString()).map { it.toRecord() }
        render()
    }

    private fun DrinkLog.toRecord() = DrinkRecord(
        name = drinkName,
        colorHex = colorHex,
        consumedMl = consumedMl,
        creditedMl = creditedMl,
        time = loggedAt?.let {
            OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalTime()
        } ?: LocalTime.now()
    )

//    // Selected chip = blue (Primary look), others = white (Secondary.Dark look).
//    private fun updateChips() {
//        chips.forEach { (button, ml) ->
//            val color = if (ml == selectedMl) R.color.primary else R.color.surface
//            button.backgroundTintList = ColorStateList.valueOf(getColor(color))
//        }
//    }

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

        if (records.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No drinks logged yet today. Tap a quick chip or + below!"
                setTextColor(getColor(R.color.muted))
                textSize = 13f
                setPadding(0, 16, 0, 8)
            }
            list.addView(emptyTv)
            return
        }

        records.forEachIndexed { index, record ->
            val row = layoutInflater.inflate(R.layout.item_drink_record, list, false)
            row.findViewById<View>(R.id.divider).isVisible = index > 0
            row.findViewById<TextView>(R.id.tvRecordName).text = record.name
            row.findViewById<TextView>(R.id.tvRecordTime).text = record.time.format(timeFormat)

            val parsedColor = runCatching { Color.parseColor(record.colorHex) }
                .getOrDefault(getColor(R.color.primary))
            row.findViewById<MaterialCardView>(R.id.cardRecordIcon).setCardBackgroundColor(parsedColor)

            row.findViewById<TextView>(R.id.tvRecordAmount).text =
                if (record.creditedMl == record.consumedMl) {
                    String.format(Locale.US, "%,d ml", record.consumedMl)
                } else {
                    String.format(Locale.US, "%,d ml (%d cr.)", record.consumedMl, record.creditedMl)
                }
            list.addView(row)
        }
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
