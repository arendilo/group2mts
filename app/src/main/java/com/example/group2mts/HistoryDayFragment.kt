package com.example.group2mts

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.DrinkLog
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

class HistoryDayFragment : Fragment(R.layout.fragment_history_day) {

    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    private val host: HistoryActivity
        get() = requireActivity() as HistoryActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dayContent = view.findViewById<View>(R.id.layoutScrollContent)
        val basePaddingTop = dayContent.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            dayContent.updatePadding(top = basePaddingTop + topInset)
            insets
        }
        ViewCompat.requestApplyInsets(view)

        //masih hardcodeddd!!
        view.findViewById<TextView>(R.id.tvStreakDays).text = "4 days"

        view.findViewById<ImageButton>(R.id.btnCalendar).setOnClickListener {
            showCalendarPicker()
        }

        view.findViewById<MaterialCardView>(R.id.cardTabDay).setOnClickListener { host.switchPeriod("Day") }
        view.findViewById<MaterialCardView>(R.id.cardTabMonth).setOnClickListener { host.switchPeriod("Month") }
        view.findViewById<MaterialCardView>(R.id.cardTabYear).setOnClickListener { host.switchPeriod("Year") }

        loadDayData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) loadDayData()
    }

    fun loadDayData() {
        val root = view ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val profile = host.repo.getProfile()
                host.fallbackGoalMl = profile.customGoalMl

                val startOfWeek = host.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val endOfWeek = startOfWeek.plusDays(6)

                val weekLogs = host.repo.getLogsBetween(startOfWeek.toString(), endOfWeek.toString())
                val datesWithLogs = weekLogs.mapNotNull { it.loggedDate }.toSet()

                renderCalendarStrip(root, datesWithLogs)

                val selectedDateStr = host.selectedDate.toString()
                val dayLogs = weekLogs.filter { it.loggedDate == selectedDateStr }
                val target = host.repo.getTarget(selectedDateStr)
                val goalMl = target?.totalGoalMl ?: host.fallbackGoalMl

                renderDayGoalCard(root, dayLogs, goalMl)
                renderTodaysRhythm(root, dayLogs)
                renderDayBreakdown(root, dayLogs)
            } catch (e: Exception) {
                renderCalendarStrip(root, emptySet())
                host.toast(e.message ?: "Failed to load history")
            }
        }
    }

    private fun renderDayGoalCard(root: View, logs: List<DrinkLog>, goalMl: Int) {
        val totalConsumed = logs.sumOf { it.consumedMl }
        val totalCredited = logs.sumOf { it.creditedMl }
        val percent = if (goalMl > 0) (totalCredited * 100 / goalMl).coerceAtMost(100) else 0

        root.findViewById<TextView>(R.id.tvHistoryTotalMl).text =
            String.format(Locale.US, "%,d ml", totalCredited)
        root.findViewById<TextView>(R.id.tvHistoryGoal).text =
            String.format(Locale.US, "of %,d ml daily goal", goalMl)
        root.findViewById<TextView>(R.id.tvHistoryPercent).text = "$percent%"
        root.findViewById<TextView>(R.id.tvConsumedCredited).text =
            String.format(Locale.US, "%,d ml consumed · %,d ml credited", totalConsumed, totalCredited)
    }

    private fun renderTodaysRhythm(root: View, logs: List<DrinkLog>) {
        val today = LocalDate.now()
        root.findViewById<TextView>(R.id.tvRhythmTitle).text =
            if (host.selectedDate == today) "Today’s rhythm" else "${host.selectedDate.format(shortMonthFormatter)} ${host.selectedDate.dayOfMonth} rhythm"

        val bucketKeys = listOf(8, 10, 12, 14, 16, 18)
        val bucketViews = mapOf(
            8 to root.findViewById<MaterialCardView>(R.id.bar08),
            10 to root.findViewById<MaterialCardView>(R.id.bar10),
            12 to root.findViewById<MaterialCardView>(R.id.bar12),
            14 to root.findViewById<MaterialCardView>(R.id.bar14),
            16 to root.findViewById<MaterialCardView>(R.id.bar16),
            18 to root.findViewById<MaterialCardView>(R.id.bar18),
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
        val emptyColor = requireContext().getColor(R.color.primary_container)
        val defaultActiveColor = requireContext().getColor(R.color.wave_blue)

        bucketKeys.forEach { key ->
            val bar = bucketViews[key] ?: return@forEach
            val totalMl = bucketTotals[key] ?: 0
            val logsInBucket = bucketLogs[key].orEmpty()

            val lp = bar.layoutParams
            if (totalMl <= 0) {
                lp.height = host.dpToPx(4f)
                bar.radius = host.dpToPx(2f).toFloat()
                bar.setCardBackgroundColor(emptyColor)
            } else {
                val ratio = (totalMl / maxScaleMl).coerceIn(0.12f, 1f)
                lp.height = host.dpToPx(maxBarHeightDp * ratio)
                bar.radius = host.dpToPx(12f).toFloat()

                val dominantHex = logsInBucket.maxByOrNull { it.creditedMl }?.colorHex
                val barColor = dominantHex?.let {
                    runCatching { Color.parseColor(it) }.getOrNull()
                } ?: defaultActiveColor
                bar.setCardBackgroundColor(barColor)
            }
            bar.layoutParams = lp
        }
    }

    private fun renderDayBreakdown(root: View, logs: List<DrinkLog>) {
        val listBreakdown = root.findViewById<LinearLayout>(R.id.listBreakdown)
        listBreakdown.removeAllViews()

        if (logs.isEmpty()) {
            listBreakdown.addView(
                host.createBreakdownCard(
                    colorHex = "#9CE3EF",
                    title = "No drinks logged",
                    subtitle = "No records found for this date",
                    rightText = "0 ml",
                    topMarginDp = 0
                )
            )
            return
        }

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

            listBreakdown.addView(
                host.createBreakdownCard(
                    colorHex = first.colorHex,
                    title = first.drinkName,
                    subtitle = subtitle,
                    rightText = String.format(Locale.US, "%,d ml", totalCredited),
                    topMarginDp = if (index == 0) 0 else 10
                )
            )
        }
    }

    private fun renderCalendarStrip(root: View, datesWithLogs: Set<String>) {
        val container = root.findViewById<LinearLayout>(R.id.layoutWeekDays)
        container.removeAllViews()

        val today = LocalDate.now()
        val startOfWeek = host.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayView = layoutInflater.inflate(R.layout.item_week_day, container, false)

            val tvDayLabel = dayView.findViewById<TextView>(R.id.tvDayLabel)
            val cardDateCircle = dayView.findViewById<MaterialCardView>(R.id.cardDateCircle)
            val tvDayNumber = dayView.findViewById<TextView>(R.id.tvDayNumber)
            val dotHasRecord = dayView.findViewById<View>(R.id.dotHasRecord)

            val isToday = date == today
            val isSelected = date == host.selectedDate
            val hasRecord = datesWithLogs.contains(date.toString())

            if (isToday) {
                tvDayLabel.text = "TODAY"
                tvDayLabel.textSize = 10f
                tvDayLabel.setTextColor(requireContext().getColor(R.color.on_background))
            } else {
                tvDayLabel.text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
                tvDayLabel.textSize = 11f
                tvDayLabel.setTextColor(requireContext().getColor(R.color.muted))
            }

            tvDayNumber.text = date.dayOfMonth.toString()

            if (isSelected) {
                cardDateCircle.setCardBackgroundColor(requireContext().getColor(R.color.streak_orange))
                tvDayNumber.setTextColor(requireContext().getColor(R.color.white))
                dotHasRecord.visibility = View.INVISIBLE
            } else {
                cardDateCircle.setCardBackgroundColor(Color.TRANSPARENT)
                tvDayNumber.setTextColor(requireContext().getColor(R.color.on_background))
                dotHasRecord.visibility = if (hasRecord) View.VISIBLE else View.INVISIBLE
            }

            dayView.setOnClickListener {
                host.selectedDate = date
                host.selectedMonth = YearMonth.from(date)
                host.selectedYear = date.year
                loadDayData()
            }

            container.addView(dayView)
        }

        val formattedDate = host.selectedDate.format(headerDateFormatter).uppercase()
        val prefix = if (host.selectedDate == today) "TODAY · " else ""
        root.findViewById<TextView>(R.id.tvSelectedDateLabel).text = "$prefix$formattedDate"
    }

    private fun showCalendarPicker() {
        val initialMillis = host.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(initialMillis)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            host.selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            host.selectedMonth = YearMonth.from(host.selectedDate)
            host.selectedYear = host.selectedDate.year
            loadDayData()
        }

        picker.show(parentFragmentManager, "HISTORY_DATE_PICKER")
    }
}