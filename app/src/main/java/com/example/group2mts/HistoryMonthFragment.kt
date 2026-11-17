package com.example.group2mts

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
import com.example.group2mts.data.DailyTarget
import com.example.group2mts.data.DrinkLog
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryMonthFragment : Fragment(R.layout.fragment_history_month) {

    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    private val host: HistoryActivity
        get() = requireActivity() as HistoryActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val monthContainer = view.findViewById<View>(R.id.layoutMonthContainer)
        val basePaddingTop = monthContainer.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            monthContainer.updatePadding(top = basePaddingTop + topInset)
            insets
        }
        ViewCompat.requestApplyInsets(view)

        view.findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            host.selectedMonth = host.selectedMonth.minusMonths(1)
            host.selectedYear = host.selectedMonth.year
            loadMonthData()
        }
        view.findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            host.selectedMonth = host.selectedMonth.plusMonths(1)
            host.selectedYear = host.selectedMonth.year
            loadMonthData()
        }

        view.findViewById<MaterialCardView>(R.id.cardMonthTabDay).setOnClickListener { host.switchPeriod("Day") }
        view.findViewById<MaterialCardView>(R.id.cardMonthTabMonth).setOnClickListener { host.switchPeriod("Month") }
        view.findViewById<MaterialCardView>(R.id.cardMonthTabYear).setOnClickListener { host.switchPeriod("Year") }

        loadMonthData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) loadMonthData()
    }

    fun loadMonthData() {
        val root = view ?: return
        root.findViewById<TextView>(R.id.tvMonthYearTitle).text =
            host.selectedMonth.format(monthYearFormatter)

        val today = LocalDate.now()
        val startOfMonth = host.selectedMonth.atDay(1)
        val endOfMonth = if (host.selectedMonth == YearMonth.from(today)) today else host.selectedMonth.atEndOfMonth()

        val shortMonth = host.selectedMonth.format(shortMonthFormatter)
        root.findViewById<TextView>(R.id.tvMonthPeriodRange).text =
            "$shortMonth 1–${endOfMonth.dayOfMonth} · hydration credited"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (startOfMonth.isAfter(today)) {
                    renderMonthStats(root, emptyList(), emptyList(), startOfMonth, endOfMonth)
                    return@launch
                }
                val logs = host.repo.getLogsBetween(startOfMonth.toString(), endOfMonth.toString())
                val targets = host.repo.getTargetsBetween(startOfMonth.toString(), endOfMonth.toString())
                renderMonthStats(root, logs, targets, startOfMonth, endOfMonth)
            } catch (_: Exception) {
            }
        }
    }

    private fun renderMonthStats(
        root: View,
        logs: List<DrinkLog>,
        targets: List<DailyTarget>,
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
            val dStr = host.selectedMonth.atDay(dayNum).toString()
            val dayCredited = logsByDate[dStr]?.sumOf { it.creditedMl } ?: 0
            val dayGoal = targetsByDate[dStr]?.totalGoalMl ?: host.fallbackGoalMl
            if (dayCredited >= dayGoal && dayGoal > 0) goalDaysMet++
            dailyValuesMl.add(dayCredited)
        }

        root.findViewById<TextView>(R.id.tvMonthTotalLitres).text =
            String.format(Locale.US, "%.1f L", totalLitres)
        root.findViewById<TextView>(R.id.tvMonthDailyAvg).text =
            String.format(Locale.US, "Daily avg: %,d ml", dailyAvgMl)
        root.findViewById<TextView>(R.id.tvMonthGoalDays).text =
            "Goal days: $goalDaysMet / $totalDaysCount"

        val container = root.findViewById<LinearLayout>(R.id.layoutMonthBarsContainer)
        container.removeAllViews()
        val maxScaleMl = maxOf(2500, dailyValuesMl.maxOrNull() ?: 2500).toFloat()
        val maxBarHeightDp = 115f
        val normalColor = requireContext().getColor(R.color.primary_container)
        val activeColor = requireContext().getColor(R.color.wave_blue)
        val horizontalMarginPx = host.dpToPx(1.5f)
        val cornerRadiusPx = host.dpToPx(4f).toFloat()

        dailyValuesMl.forEachIndexed { index, ml ->
            val heightRatio = if (ml <= 0) 0.04f else (ml / maxScaleMl).coerceIn(0.06f, 1f)
            val barHeightPx = host.dpToPx(maxBarHeightDp * heightRatio)

            val barCard = MaterialCardView(requireContext()).apply {
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

        val monthBreakdown = root.findViewById<LinearLayout>(R.id.listMonthBreakdown)
        monthBreakdown.removeAllViews()

        if (logs.isEmpty()) {
            monthBreakdown.addView(
                host.createBreakdownCard("#9CE3EF", "No drinks this period", null, "0.0 L", 0)
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
                host.createBreakdownCard(
                    colorHex = first.colorHex,
                    title = first.drinkName,
                    subtitle = "${group.size} logs this period",
                    rightText = String.format(Locale.US, "%.1f L", litres),
                    topMarginDp = if (index == 0) 0 else 10
                )
            )
        }
    }
}