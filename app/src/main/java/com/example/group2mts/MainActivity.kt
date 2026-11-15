package com.example.group2mts

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// One "sip" the user logged.
data class DrinkRecord(val name: String, val amountMl: Int, val time: LocalTime)

class MainActivity : AppCompatActivity() {

    // ---- State (kept in memory for now; no database yet) ----
    private val dailyGoalMl = 2000
    private var selectedMl = 200 // which chip is active

    // Newest first. Sample data = the numbers from the Figma design (500 + 350 + 350 = 1200).
    private val records = mutableListOf(
        DrinkRecord("Water", 500, LocalTime.of(11, 30)),
        DrinkRecord("Water", 350, LocalTime.of(10, 0)),
        DrinkRecord("Water", 350, LocalTime.of(8, 15)),
    )

    private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    // Each chip button paired with the amount it stands for.
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
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            records.add(0, DrinkRecord("Water", selectedMl, LocalTime.now()))
            render()
        }

        // TODO: nav clicks (navHistory, navGoals, navProfile) once those screens exist.

        updateChips()
        render()
    }

    // Selected chip = blue (Primary look), others = white (Secondary.Dark look).
    private fun updateChips() {
        chips.forEach { (button, ml) ->
            val color = if (ml == selectedMl) R.color.primary else R.color.surface
            button.backgroundTintList = ColorStateList.valueOf(getColor(color))
        }
    }

    // Re-draws everything that depends on `records`. Call it after any change.
    private fun render() {
        val total = records.sumOf { it.amountMl }
        val percent = (total * 100 / dailyGoalMl).coerceAtMost(100)

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
                String.format(Locale.US, "%,d ml", record.amountMl)
            list.addView(row)
        }
    }
}
