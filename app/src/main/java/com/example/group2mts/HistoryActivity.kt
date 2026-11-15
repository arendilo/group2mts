package com.example.group2mts

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private var selectedDate: LocalDate = LocalDate.now()
    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        val scrollContent = findViewById<View>(R.id.layoutScrollContent)
        val contentBasePaddingTop = scrollContent.paddingTop
        val navBar = findViewById<View>(R.id.navBar)
        val navBarBasePadding = navBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            scrollContent.updatePadding(top = contentBasePaddingTop + systemBars.top)
            navBar.updatePadding(bottom = navBarBasePadding + systemBars.bottom)
            insets
        }

        // 1. Klik ikon kalender di pojok kanan atas
        findViewById<ImageButton>(R.id.btnCalendar).setOnClickListener {
            showCalendarPicker()
        }

        // 2. Klik tab Day / Month / Year
        setupPeriodTabs()

        // 3. Klik tombol Home di navbar bawah
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Render kalender mingguan
        renderCalendarStrip()
    }

    private fun setupPeriodTabs() {
        val cardDay = findViewById<MaterialCardView>(R.id.cardTabDay)
        val cardMonth = findViewById<MaterialCardView>(R.id.cardTabMonth)
        val cardYear = findViewById<MaterialCardView>(R.id.cardTabYear)

        val tabDay = findViewById<TextView>(R.id.tabDay)
        val tabMonth = findViewById<TextView>(R.id.tabMonth)
        val tabYear = findViewById<TextView>(R.id.tabYear)
        val tvRhythmTitle = findViewById<TextView>(R.id.tvRhythmTitle)

        fun selectTab(selected: String) {
            val activeBg = getColor(R.color.wave_blue)
            val inactiveBg = Color.TRANSPARENT
            val activeText = getColor(R.color.white)
            val inactiveText = getColor(R.color.on_background)

            cardDay.setCardBackgroundColor(if (selected == "Day") activeBg else inactiveBg)
            tabDay.setTextColor(if (selected == "Day") activeText else inactiveText)

            cardMonth.setCardBackgroundColor(if (selected == "Month") activeBg else inactiveBg)
            tabMonth.setTextColor(if (selected == "Month") activeText else inactiveText)

            cardYear.setCardBackgroundColor(if (selected == "Year") activeBg else inactiveBg)
            tabYear.setTextColor(if (selected == "Year") activeText else inactiveText)

            tvRhythmTitle.text = when (selected) {
                "Month" -> "This month’s rhythm"
                "Year" -> "This year’s rhythm"
                else -> "Today’s rhythm"
            }
        }

        cardDay.setOnClickListener { selectTab("Day") }
        cardMonth.setOnClickListener { selectTab("Month") }
        cardYear.setOnClickListener { selectTab("Year") }
    }

    private fun renderCalendarStrip() {
        val container = findViewById<LinearLayout>(R.id.layoutWeekDays)
        container.removeAllViews()

        val today = LocalDate.now()
        val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayView = layoutInflater.inflate(R.layout.item_week_day, container, false)

            val tvDayLabel = dayView.findViewById<TextView>(R.id.tvDayLabel)
            val cardDateCircle = dayView.findViewById<MaterialCardView>(R.id.cardDateCircle)
            val tvDayNumber = dayView.findViewById<TextView>(R.id.tvDayNumber)
            val dotHasRecord = dayView.findViewById<View>(R.id.dotHasRecord)

            val isToday = date == today
            val isSelected = date == selectedDate

            if (isToday) {
                tvDayLabel.text = "TODAY"
                tvDayLabel.textSize = 10f
                tvDayLabel.setTextColor(getColor(R.color.on_background))
            } else {
                tvDayLabel.text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
                tvDayLabel.textSize = 11f
                tvDayLabel.setTextColor(getColor(R.color.muted))
            }

            tvDayNumber.text = date.dayOfMonth.toString()

            if (isSelected) {
                cardDateCircle.setCardBackgroundColor(getColor(R.color.streak_orange))
                tvDayNumber.setTextColor(getColor(R.color.white))
                dotHasRecord.visibility = View.INVISIBLE
            } else {
                cardDateCircle.setCardBackgroundColor(Color.TRANSPARENT)
                tvDayNumber.setTextColor(getColor(R.color.on_background))
                dotHasRecord.visibility = if (!date.isAfter(today)) View.VISIBLE else View.INVISIBLE
            }

            dayView.setOnClickListener {
                selectedDate = date
                renderCalendarStrip()
            }

            container.addView(dayView)
        }

        val formattedDate = selectedDate.format(headerDateFormatter).uppercase()
        val prefix = if (selectedDate == today) "TODAY · " else ""
        findViewById<TextView>(R.id.tvSelectedDateLabel).text = "$prefix$formattedDate"
    }

    private fun showCalendarPicker() {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(initialMillis)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            renderCalendarStrip()
        }

        picker.show(supportFragmentManager, "HISTORY_DATE_PICKER")
    }
}