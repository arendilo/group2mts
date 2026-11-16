package com.example.group2mts

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.DrinkLog
import com.example.group2mts.data.HydrationRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private val repo = HydrationRepository()
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedMonth: YearMonth = YearMonth.from(selectedDate)
    private var fallbackGoalMl: Int = 2000
    private var isFirstResume = true

    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

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

        // Streak - future development
        findViewById<TextView>(R.id.tvStreakDays).text = "4 days"

        // Date picker
        findViewById<ImageButton>(R.id.btnCalendar).setOnClickListener {
            showCalendarPicker()
        }

        // Month navigation
        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            selectedMonth = selectedMonth.minusMonths(1)
            loadMonthData()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            selectedMonth = selectedMonth.plusMonths(1)
            loadMonthData()
        }

        // Period tabs
        setupPeriodTabs()

        // Bottom navigation
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navGoals).setOnClickListener {
            startActivity(Intent(this, DrinkShelfActivity::class.java))
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, LogDrinkActivity::class.java))
        }

        loadDayData()
        loadMonthData()
    }

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        loadDayData()
        loadMonthData()
    }

    private fun setupPeriodTabs() {
        val scrollHistory = findViewById<NestedScrollView>(R.id.scrollHistory)
        val dayContainer = findViewById<View>(R.id.layoutDayContainer)
        val monthContainer = findViewById<View>(R.id.layoutMonthContainer)
        val yearContainer = findViewById<View>(R.id.layoutYearContainer)

        fun switchMode(mode: String) {
            dayContainer.isVisible = (mode == "Day")
            monthContainer.isVisible = (mode == "Month")
            yearContainer.isVisible = (mode == "Year")
            if (mode == "Month") loadMonthData()
            if (mode == "Day") loadDayData()
            scrollHistory.scrollTo(0, 0)
        }

        findViewById<MaterialCardView>(R.id.cardTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardTabMonth).setOnClickListener { switchMode("Month") }
        findViewById<MaterialCardView>(R.id.cardTabYear).setOnClickListener { switchMode("Year") }

        findViewById<MaterialCardView>(R.id.cardMonthTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardMonthTabMonth).setOnClickListener { switchMode("Month") }
        findViewById<MaterialCardView>(R.id.cardMonthTabYear).setOnClickListener { switchMode("Year") }

        findViewById<MaterialCardView>(R.id.cardYearTabDay).setOnClickListener { switchMode("Day") }
        findViewById<MaterialCardView>(R.id.cardYearTabMonth).setOnClickListener { switchMode("Month") }
        findViewById<MaterialCardView>(R.id.cardYearTabYear).setOnClickListener { switchMode("Year") }
    }

    private fun loadDayData() {
        lifecycleScope.launch {
            try {
                val profile = repo.getProfile()
                fallbackGoalMl = profile.customGoalMl

                val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val endOfWeek = startOfWeek.plusDays(6)

                val weekLogs = repo.getLogsBetween(startOfWeek.toString(), endOfWeek.toString())
                val datesWithLogs = weekLogs.mapNotNull { it.loggedDate }.toSet()

                renderCalendarStrip(datesWithLogs)

                val selectedDateStr = selectedDate.toString()
                val dayLogs = weekLogs.filter { it.loggedDate == selectedDateStr }
                val target = repo.getTarget(selectedDateStr)
                val goalMl = target?.totalGoalMl ?: fallbackGoalMl

                renderDayGoalCard(dayLogs, goalMl)
                renderTodaysRhythm(dayLogs)
                renderDayBreakdown(dayLogs)
            } catch (e: Exception) {
                renderCalendarStrip(emptySet())
                toast(e.message ?: "Failed to load history")
            }
        }
    }

    private fun renderDayGoalCard(logs: List<DrinkLog>, goalMl: Int) {
        val totalConsumed = logs.sumOf { it.consumedMl }
        val totalCredited = logs.sumOf { it.creditedMl }
        val percent = if (goalMl > 0) (totalCredited * 100 / goalMl).coerceAtMost(100) else 0

        findViewById<TextView>(R.id.tvHistoryTotalMl).text =
            String.format(Locale.US, "%,d ml", totalCredited)
        findViewById<TextView>(R.id.tvHistoryGoal).text =
            String.format(Locale.US, "of %,d ml daily goal", goalMl)
        findViewById<TextView>(R.id.tvHistoryPercent).text = "$percent%"
        findViewById<TextView>(R.id.tvConsumedCredited).text =
            String.format(Locale.US, "%,d ml consumed · %,d ml credited", totalConsumed, totalCredited)
    }

    private fun renderTodaysRhythm(logs: List<DrinkLog>) {
        val today = LocalDate.now()
        findViewById<TextView>(R.id.tvRhythmTitle).text =
            if (selectedDate == today) "Today’s rhythm" else "${selectedDate.format(shortMonthFormatter)} ${selectedDate.dayOfMonth} rhythm"

        // 6 two-hour buckets matching bar08, bar10, bar12, bar14, bar16, bar18
        val bucketKeys = listOf(8, 10, 12, 14, 16, 18)
        val bucketViews = mapOf(
            8 to findViewById<MaterialCardView>(R.id.bar08),
            10 to findViewById<MaterialCardView>(R.id.bar10),
            12 to findViewById<MaterialCardView>(R.id.bar12),
            14 to findViewById<MaterialCardView>(R.id.bar14),
            16 to findViewById<MaterialCardView>(R.id.bar16),
            18 to findViewById<MaterialCardView>(R.id.bar18),
        )

        val bucketLogs = bucketKeys.associateWith { mutableListOf<DrinkLog>() }
        logs.forEach { log ->
            val hour = log.loggedAt?.let {
                OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).hour
            } ?: 12
            val bucket = when {
                hour < 10 -> 8
                hour < 12 -> 10
                hour < 14 -> 12
                hour < 16 -> 14
                hour < 18 -> 16
                else -> 18
            }
            bucketLogs[bucket]?.add(log)
        }

        val bucketTotals = bucketKeys.associateWith { key ->
            bucketLogs[key]?.sumOf { it.creditedMl } ?: 0
        }
        val maxScaleMl = maxOf(800, bucketTotals.values.maxOrNull() ?: 800).toFloat()
        val maxBarHeightDp = 114f
        val emptyColor = getColor(R.color.primary_container)
        val defaultActiveColor = getColor(R.color.wave_blue)

        bucketKeys.forEach { key ->
            val bar = bucketViews[key] ?: return@forEach
            val totalMl = bucketTotals[key] ?: 0
            val logsInBucket = bucketLogs[key].orEmpty()

            val lp = bar.layoutParams
            if (totalMl <= 0) {
                lp.height = dpToPx(4f)
                bar.radius = dpToPx(2f).toFloat()
                bar.setCardBackgroundColor(emptyColor)
            } else {
                val ratio = (totalMl / maxScaleMl).coerceIn(0.12f, 1f)
                lp.height = dpToPx(maxBarHeightDp * ratio)
                bar.radius = dpToPx(12f).toFloat()

                // Use the color of the largest drink in that time bucket
                val dominantHex = logsInBucket.maxByOrNull { it.creditedMl }?.colorHex
                val barColor = dominantHex?.let {
                    runCatching { Color.parseColor(it) }.getOrNull()
                } ?: defaultActiveColor
                bar.setCardBackgroundColor(barColor)
            }
            bar.layoutParams = lp
        }
    }

    private fun renderDayBreakdown(logs: List<DrinkLog>) {
        val listBreakdown = findViewById<LinearLayout>(R.id.listBreakdown)
        listBreakdown.removeAllViews()

        if (logs.isEmpty()) {
            val emptyCard = createBreakdownCard(
                colorHex = "#9CE3EF",
                title = "No drinks logged",
                subtitle = "No records found for this date",
                rightText = "0 ml",
                topMarginDp = 0
            )
            listBreakdown.addView(emptyCard)
            return
        }

        // Group by drinkName (preserving order of total credited volume)
        val grouped = logs.groupBy { it.drinkName }
            .values
            .sortedByDescending { group -> group.sumOf { it.creditedMl } }

        grouped.forEachIndexed { index, group ->
            val first = group.first()
            val totalConsumed = group.sumOf { it.consumedMl }
            val totalCredited = group.sumOf { it.creditedMl }

            val subtitle = if (group.size == 1) {
                val timeText = first.loggedAt?.let {
                    OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalTime().format(timeFormatter)
                } ?: LocalTime.now().format(timeFormatter)
                String.format(Locale.US, "%,d ml · %s", totalConsumed, timeText)
            } else {
                String.format(Locale.US, "%,d ml · %d drinks", totalConsumed, group.size)
            }

            val card = createBreakdownCard(
                colorHex = first.colorHex,
                title = first.drinkName,
                subtitle = subtitle,
                rightText = String.format(Locale.US, "%,d ml", totalCredited),
                topMarginDp = if (index == 0) 0 else 10
            )
            listBreakdown.addView(card)
        }
    }

    private fun createBreakdownCard(
        colorHex: String,
        title: String,
        subtitle: String?,
        rightText: String,
        topMarginDp: Int
    ): MaterialCardView {
        val nunitoBold = ResourcesCompat.getFont(this, R.font.nunito_bold)
        val nunitoExtraBold = ResourcesCompat.getFont(this, R.font.nunito_extrabold)
        val nunitoRegular = ResourcesCompat.getFont(this, R.font.nunito)

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (topMarginDp > 0) topMargin = dpToPx(topMarginDp.toFloat())
            }
            radius = dpToPx(16f).toFloat()
            cardElevation = dpToPx(4f).toFloat()
            strokeWidth = 0
            setCardBackgroundColor(getColor(R.color.white))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = dpToPx(16f)
            setPadding(pad, pad, pad, pad)
        }

        val accentColor = runCatching { Color.parseColor(colorHex) }
            .getOrDefault(getColor(R.color.wave_blue))

        val pill = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6f), dpToPx(34f))
            radius = dpToPx(3f).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(accentColor)
        }
        row.addView(pill)

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(14f)
            }
        }

        val tvTitle = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(getColor(R.color.on_background))
            typeface = nunitoBold ?: Typeface.DEFAULT_BOLD
        }
        textCol.addView(tvTitle)

        if (!subtitle.isNullOrBlank()) {
            val tvSub = TextView(this).apply {
                text = subtitle
                textSize = 12f
                setTextColor(getColor(R.color.muted))
                typeface = nunitoRegular ?: Typeface.DEFAULT
            }
            textCol.addView(tvSub)
        }
        row.addView(textCol)

        val tvRight = TextView(this).apply {
            text = rightText
            textSize = 15f
            setTextColor(getColor(R.color.on_background))
            typeface = nunitoExtraBold ?: Typeface.DEFAULT_BOLD
        }
        row.addView(tvRight)

        card.addView(row)
        return card
    }

    private fun loadMonthData() {
        findViewById<TextView>(R.id.tvMonthYearTitle).text =
            selectedMonth.format(monthYearFormatter)

        val today = LocalDate.now()
        val startOfMonth = selectedMonth.atDay(1)
        val endOfMonth = if (selectedMonth == YearMonth.from(today)) today else selectedMonth.atEndOfMonth()

        val shortMonth = selectedMonth.format(shortMonthFormatter)
        findViewById<TextView>(R.id.tvMonthPeriodRange).text =
            "$shortMonth 1–${endOfMonth.dayOfMonth} · hydration credited"

        lifecycleScope.launch {
            try {
                if (startOfMonth.isAfter(today)) {
                    renderMonthStats(emptyList(), emptyList(), startOfMonth, endOfMonth)
                    return@launch
                }
                val logs = repo.getLogsBetween(startOfMonth.toString(), endOfMonth.toString())
                val targets = repo.getTargetsBetween(startOfMonth.toString(), endOfMonth.toString())
                renderMonthStats(logs, targets, startOfMonth, endOfMonth)
            } catch (_: Exception) {
                // Keep UI responsive on network hiccup
            }
        }
    }

    private fun renderMonthStats(
        logs: List<DrinkLog>,
        targets: List<com.example.group2mts.data.DailyTarget>,
        startOfMonth: LocalDate,
        endOfMonth: LocalDate
    ) {
        val totalDaysCount = (endOfMonth.dayOfMonth - startOfMonth.dayOfMonth + 1).coerceAtLeast(1)
        val totalCreditedMl = logs.sumOf { it.creditedMl }
        val totalLitres = totalCreditedMl / 1000.0
        val dailyAvgMl = totalCreditedMl / totalDaysCount

        val logsByDate = logs.groupBy { it.loggedDate }
        val targetsByDate = targets.associateBy { it.targetDate }

        var goalDaysMet = 0
        val dailyValuesMl = mutableListOf<Int>()
        for (dayNum in 1..endOfMonth.dayOfMonth) {
            val dStr = selectedMonth.atDay(dayNum).toString()
            val dayCredited = logsByDate[dStr]?.sumOf { it.creditedMl } ?: 0
            val dayGoal = targetsByDate[dStr]?.totalGoalMl ?: fallbackGoalMl
            if (dayCredited >= dayGoal && dayGoal > 0) goalDaysMet++
            dailyValuesMl.add(dayCredited)
        }

        findViewById<TextView>(R.id.tvMonthTotalLitres).text =
            String.format(Locale.US, "%.1f L", totalLitres)
        findViewById<TextView>(R.id.tvMonthDailyAvg).text =
            String.format(Locale.US, "Daily avg: %,d ml", dailyAvgMl)
        findViewById<TextView>(R.id.tvMonthGoalDays).text =
            "Goal days: $goalDaysMet / $totalDaysCount"

        // Render bars
        val container = findViewById<LinearLayout>(R.id.layoutMonthBarsContainer)
        container.removeAllViews()
        val maxScaleMl = maxOf(2500, dailyValuesMl.maxOrNull() ?: 2500).toFloat()
        val maxBarHeightDp = 115f
        val normalColor = getColor(R.color.primary_container)
        val activeColor = getColor(R.color.wave_blue)
        val horizontalMarginPx = dpToPx(1.5f)
        val cornerRadiusPx = dpToPx(4f).toFloat()

        dailyValuesMl.forEachIndexed { index, ml ->
            val heightRatio = if (ml <= 0) 0.04f else (ml / maxScaleMl).coerceIn(0.06f, 1f)
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

        // Render Month Drink Breakdown
        val monthBreakdown = findViewById<LinearLayout>(R.id.listMonthBreakdown)
        monthBreakdown.removeAllViews()

        if (logs.isEmpty()) {
            monthBreakdown.addView(
                createBreakdownCard("#9CE3EF", "No drinks this period", null, "0.0 L", 0)
            )
            return
        }

        val grouped = logs.groupBy { it.drinkName }
            .values
            .sortedByDescending { group -> group.sumOf { it.creditedMl } }

        grouped.forEachIndexed { index, group ->
            val first = group.first()
            val litres = group.sumOf { it.creditedMl } / 1000.0
            monthBreakdown.addView(
                createBreakdownCard(
                    colorHex = first.colorHex,
                    title = first.drinkName,
                    subtitle = "${group.size} logs this period",
                    rightText = String.format(Locale.US, "%.1f L", litres),
                    topMarginDp = if (index == 0) 0 else 10
                )
            )
        }
    }


    private fun renderCalendarStrip(datesWithLogs: Set<String>) {
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
            val hasRecord = datesWithLogs.contains(date.toString())

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
                dotHasRecord.visibility = if (hasRecord) View.VISIBLE else View.INVISIBLE
            }

            dayView.setOnClickListener {
                selectedDate = date
                selectedMonth = YearMonth.from(date)
                loadDayData()
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
            loadDayData()
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

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}