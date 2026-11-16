package com.example.group2mts

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedMonth: YearMonth = YearMonth.from(selectedDate)

    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        // Window insets
        val dayContent = findViewById<View>(R.id.layoutScrollContent)
        val monthContainer = findViewById<View>(R.id.layoutMonthContainer)
        val dayBasePaddingTop = dayContent.paddingTop
        val monthBasePaddingTop = monthContainer.paddingTop
        val navBar = findViewById<View>(R.id.navBar)
        val navBarBasePadding = navBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            dayContent.updatePadding(top = dayBasePaddingTop + systemBars.top)
            monthContainer.updatePadding(top = monthBasePaddingTop + systemBars.top)
            navBar.updatePadding(bottom = navBarBasePadding + systemBars.bottom)
            insets
        }

        // Date picker
        findViewById<ImageButton>(R.id.btnCalendar).setOnClickListener {
            showCalendarPicker()
        }

        // Month navigation
        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            selectedMonth = selectedMonth.minusMonths(1)
            renderMonthSection()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            selectedMonth = selectedMonth.plusMonths(1)
            renderMonthSection()
        }

        // Period tabs
        setupPeriodTabs()

        // Bottom navigation
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        renderCalendarStrip()
        renderMonthSection()
    }

    private fun setupPeriodTabs() {
        val scrollHistory = findViewById<NestedScrollView>(R.id.scrollHistory)
        val dayContainer = findViewById<View>(R.id.layoutDayContainer)
        val monthContainer = findViewById<View>(R.id.layoutMonthContainer)

        fun switchMode(mode: String) {
            if (mode == "Month") {
                dayContainer.isVisible = false
                monthContainer.isVisible = true
                renderMonthSection()
            } else {
                monthContainer.isVisible = false
                dayContainer.isVisible = true
            }
            scrollHistory.scrollTo(0, 0)
        }

        // Day view tabs
        findViewById<MaterialCardView>(R.id.cardTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardTabMonth).setOnClickListener { switchMode("Month") }

        // Month view tabs
        findViewById<MaterialCardView>(R.id.cardMonthTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardMonthTabMonth).setOnClickListener { switchMode("Month") }
    }

    private fun renderMonthSection() {
        findViewById<TextView>(R.id.tvMonthYearTitle).text =
            selectedMonth.format(monthYearFormatter)

        val shortMonth = selectedMonth.format(shortMonthFormatter)
        findViewById<TextView>(R.id.tvMonthPeriodRange).text =
            "$shortMonth 1–14 · hydration credited"

        renderMonthBars()
    }

    private fun renderMonthBars() {
        val container = findViewById<LinearLayout>(R.id.layoutMonthBarsContainer)
        container.removeAllViews()

        // Chart data
        val dailyValuesMl = listOf(
            1650, 1850, 2150, 1950, 2050, 2100, 2250,
            2150, 2350, 2300, 2150, 2150, 1650, 1200
        )
        val maxScaleMl = 2500f
        val maxBarHeightDp = 115f

        val normalColor = getColor(R.color.primary_container)
        val activeColor = getColor(R.color.wave_blue)
        val horizontalMarginPx = dpToPx(2.5f)
        val cornerRadiusPx = dpToPx(6f).toFloat()

        dailyValuesMl.forEachIndexed { index, ml ->
            val heightRatio = (ml / maxScaleMl).coerceIn(0.05f, 1f)
            val barHeightPx = dpToPx(maxBarHeightDp * heightRatio)

            val barCard = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, barHeightPx, 1f).apply {
                    marginStart = horizontalMarginPx
                    marginEnd = horizontalMarginPx
                }
                radius = cornerRadiusPx
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(if (index == dailyValuesMl.lastIndex) activeColor else normalColor)
            }
            container.addView(barCard)
        }
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
                selectedMonth = YearMonth.from(date)
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
            selectedMonth = YearMonth.from(selectedDate)
            renderCalendarStrip()
        }

        picker.show(supportFragmentManager, "HISTORY_DATE_PICKER")
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }
}