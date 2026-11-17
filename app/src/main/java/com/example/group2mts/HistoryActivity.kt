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
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedMonth: YearMonth = YearMonth.from(selectedDate)
    private var selectedYear: Int = selectedDate.year

    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        // Window insets
        val dayContent = findViewById<View>(R.id.layoutScrollContent)
        val monthContainer = findViewById<View>(R.id.layoutMonthContainer)
        val yearContainer = findViewById<View>(R.id.layoutYearContainer)
        val dayBasePaddingTop = dayContent.paddingTop
        val monthBasePaddingTop = monthContainer.paddingTop
        val yearBasePaddingTop = yearContainer.paddingTop
        val navBar = findViewById<View>(R.id.navBar)
        val navBarBasePadding = navBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            dayContent.updatePadding(top = dayBasePaddingTop + systemBars.top)
            monthContainer.updatePadding(top = monthBasePaddingTop + systemBars.top)
            yearContainer.updatePadding(top = yearBasePaddingTop + systemBars.top)
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

        // Year navigation
        findViewById<ImageButton>(R.id.btnPrevYear).setOnClickListener {
            selectedYear -= 1
            renderYearSection()
        }
        findViewById<ImageButton>(R.id.btnNextYear).setOnClickListener {
            selectedYear += 1
            renderYearSection()
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
        renderYearSection()
    }

    private fun setupPeriodTabs() {
        val scrollHistory = findViewById<NestedScrollView>(R.id.scrollHistory)
        val dayContainer = findViewById<View>(R.id.layoutDayContainer)
        val monthContainer = findViewById<View>(R.id.layoutMonthContainer)
        val yearContainer = findViewById<View>(R.id.layoutYearContainer)

        fun switchMode(mode: String) {
            dayContainer.isVisible = mode == "Day"
            monthContainer.isVisible = mode == "Month"
            yearContainer.isVisible = mode == "Year"

            when (mode) {
                "Month" -> renderMonthSection()
                "Year" -> renderYearSection()
            }
            scrollHistory.scrollTo(0, 0)
        }

        // Day view tabs
        findViewById<MaterialCardView>(R.id.cardTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardTabMonth).setOnClickListener { switchMode("Month") }
        findViewById<MaterialCardView>(R.id.cardTabYear).setOnClickListener { switchMode("Year") }

        // Month view tabs
        findViewById<MaterialCardView>(R.id.cardMonthTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardMonthTabMonth).setOnClickListener { switchMode("Month") }
        findViewById<MaterialCardView>(R.id.cardMonthTabYear).setOnClickListener { switchMode("Year") }

        // Year view tabs
        findViewById<MaterialCardView>(R.id.cardYearTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardYearTabMonth).setOnClickListener { switchMode("Month") }
        findViewById<MaterialCardView>(R.id.cardYearTabYear).setOnClickListener { switchMode("Year") }
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

    private fun renderYearSection() {
        findViewById<TextView>(R.id.tvYearTitle).text = selectedYear.toString()

        val monthlyLitres = if (selectedYear == 2026) {
            listOf(55.8f, 52.8f, 58.8f, 56.8f, 60.8f, 58.8f, 64.8f, 62.8f, 57.8f, 54.0f, 27.3f, 0f)
        } else {
            listOf(52.0f, 50.5f, 56.0f, 54.5f, 58.0f, 61.0f, 63.5f, 60.0f, 55.0f, 53.5f, 51.0f, 54.0f)
        }

        val totalLitres = monthlyLitres.sum()
        val bestMonthIndex = monthlyLitres.indices.maxByOrNull { monthlyLitres[it] } ?: 6
        val bestMonthName = Month.of(bestMonthIndex + 1)
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        val elapsedDays = if (selectedYear == 2026) 318 else 365
        val dailyAvgMl = ((totalLitres * 1000f) / elapsedDays).toInt()
        val formattedAvg = numberFormat.format(dailyAvgMl)

        val periodRange = if (selectedYear == 2026) {
            "Jan 1–Nov 14 · hydration credited"
        } else {
            "Jan 1–Dec 31 · hydration credited"
        }

        val waterLitres = if (selectedYear == 2026) 500f else totalLitres * 0.82f
        val coffeeLitres = if (selectedYear == 2026) 80f else totalLitres * 0.13f
        val teaLitres = if (selectedYear == 2026) 30.5f else totalLitres * 0.05f

        findViewById<TextView>(R.id.tvYearTotalLitres).text = formatLitres(totalLitres)
        findViewById<TextView>(R.id.tvYearPeriodRange).text = periodRange
        findViewById<TextView>(R.id.tvYearDailyAvg).text = "Daily avg: $formattedAvg ml"
        findViewById<TextView>(R.id.tvYearBestMonth).text =
            getString(R.string.best_month_format, bestMonthName)

        findViewById<TextView>(R.id.tvYearWaterLitres).text = formatLitres(waterLitres)
        findViewById<TextView>(R.id.tvYearCoffeeLitres).text = formatLitres(coffeeLitres)
        findViewById<TextView>(R.id.tvYearTeaLitres).text = formatLitres(teaLitres)

        renderYearBars(monthlyLitres, bestMonthIndex)
    }

    private fun renderYearBars(monthlyLitres: List<Float>, bestMonthIndex: Int) {
        val container = findViewById<LinearLayout>(R.id.layoutYearBarsContainer)
        container.removeAllViews()

        val maxScaleLitres = 70f
        val maxBarHeightDp = 115f

        val normalColor = getColor(R.color.primary_container)
        val activeColor = getColor(R.color.wave_blue)
        val emptyColor = getColor(R.color.chart_grid_line)
        val horizontalMarginPx = dpToPx(3.5f)
        val cornerRadiusPx = dpToPx(7f).toFloat()

        monthlyLitres.forEachIndexed { index, litres ->
            val isZero = litres <= 0f
            val barHeightPx = if (isZero) {
                dpToPx(2f)
            } else {
                val heightRatio = (litres / maxScaleLitres).coerceIn(0.05f, 1f)
                dpToPx(maxBarHeightDp * heightRatio)
            }

            val barColor = when {
                isZero -> emptyColor
                index == bestMonthIndex -> activeColor
                else -> normalColor
            }

            val barCard = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, barHeightPx, 1f).apply {
                    marginStart = horizontalMarginPx
                    marginEnd = horizontalMarginPx
                }
                radius = if (isZero) dpToPx(1f).toFloat() else cornerRadiusPx
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(barColor)
            }
            container.addView(barCard)
        }
    }

    private fun formatLitres(litres: Float): String {
        return if (litres % 1f == 0f) {
            "${litres.toInt()} L"
        } else {
            String.format(Locale.US, "%.1f L", litres)
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
                selectedYear = date.year
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
            selectedYear = selectedDate.year
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