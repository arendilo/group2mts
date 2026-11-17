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
import com.example.group2mts.data.DrinkLog
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class HistoryYearFragment : Fragment(R.layout.fragment_history_year) {

    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    private val host: HistoryActivity
        get() = requireActivity() as HistoryActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val yearContainer = view.findViewById<View>(R.id.layoutYearContainer)
        val basePaddingTop = yearContainer.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            yearContainer.updatePadding(top = basePaddingTop + topInset)
            insets
        }
        ViewCompat.requestApplyInsets(view)

        view.findViewById<ImageButton>(R.id.btnPrevYear).setOnClickListener {
            host.selectedYear -= 1
            loadYearData()
        }
        view.findViewById<ImageButton>(R.id.btnNextYear).setOnClickListener {
            host.selectedYear += 1
            loadYearData()
        }

        view.findViewById<MaterialCardView>(R.id.cardYearTabDay).setOnClickListener { host.switchPeriod("Day") }
        view.findViewById<MaterialCardView>(R.id.cardYearTabMonth).setOnClickListener { host.switchPeriod("Month") }
        view.findViewById<MaterialCardView>(R.id.cardYearTabYear).setOnClickListener { host.switchPeriod("Year") }

        loadYearData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) loadYearData()
    }

    fun loadYearData() {
        val root = view ?: return
        val year = host.selectedYear
        root.findViewById<TextView>(R.id.tvYearTitle).text = year.toString()

        val today = LocalDate.now()
        val startOfYear = LocalDate.of(year, 1, 1)
        val endOfYear = if (year == today.year) today else LocalDate.of(year, 12, 31)

        val periodRange = if (year == today.year) {
            "Jan 1–${today.format(shortMonthFormatter)} ${today.dayOfMonth} · hydration credited"
        } else {
            "Jan 1–Dec 31 · hydration credited"
        }
        root.findViewById<TextView>(R.id.tvYearPeriodRange).text = periodRange

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (startOfYear.isAfter(today)) {
                    renderYearStats(root, emptyList(), startOfYear, endOfYear, false)
                    return@launch
                }
                val logs = host.repo.getLogsBetween(startOfYear.toString(), endOfYear.toString())
                renderYearStats(root, logs, startOfYear, endOfYear, true)
            } catch (_: Exception) {
            }
        }
    }

    private fun renderYearStats(
        root: View,
        logs: List<DrinkLog>,
        startOfYear: LocalDate,
        endOfYear: LocalDate,
        hasElapsed: Boolean
    ) {
        val totalCreditedMl = logs.sumOf { it.creditedMl }
        val totalLitres = totalCreditedMl / 1000.0
        val elapsedDays = if (hasElapsed) {
            (ChronoUnit.DAYS.between(startOfYear, endOfYear).toInt() + 1).coerceAtLeast(1)
        } else {
            1
        }
        val dailyAvgMl = if (hasElapsed) totalCreditedMl / elapsedDays else 0

        val monthlyTotalsMl = IntArray(12)
        logs.forEach { log ->
            val monthIndex = log.loggedDate?.let {
                runCatching { LocalDate.parse(it).monthValue - 1 }.getOrNull()
            }
            if (monthIndex != null && monthIndex in 0..11) {
                monthlyTotalsMl[monthIndex] += log.creditedMl
            }
        }

        val monthlyLitres = monthlyTotalsMl.map { it / 1000f }
        val bestMonthIndex = monthlyLitres.indices.maxByOrNull { monthlyLitres[it] } ?: 0
        val bestMonthName = if (monthlyLitres[bestMonthIndex] > 0f) {
            Month.of(bestMonthIndex + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        } else {
            "-"
        }

        root.findViewById<TextView>(R.id.tvYearTotalLitres).text = formatLitres(totalLitres)
        root.findViewById<TextView>(R.id.tvYearDailyAvg).text =
            String.format(Locale.US, "Daily avg: %,d ml", dailyAvgMl)
        root.findViewById<TextView>(R.id.tvYearBestMonth).text = "Best month: $bestMonthName"

        val container = root.findViewById<LinearLayout>(R.id.layoutYearBarsContainer)
        container.removeAllViews()

        val maxScaleLitres = maxOf(70f, monthlyLitres.maxOrNull() ?: 70f)
        val maxBarHeightDp = 115f
        val normalColor = requireContext().getColor(R.color.primary_container)
        val activeColor = requireContext().getColor(R.color.wave_blue)
        val emptyColor = requireContext().getColor(R.color.chart_grid_line)
        val horizontalMarginPx = host.dpToPx(3.5f)
        val cornerRadiusPx = host.dpToPx(7f).toFloat()

        monthlyLitres.forEachIndexed { index, litres ->
            val isZero = litres <= 0f
            val barHeightPx = if (isZero) {
                host.dpToPx(2f)
            } else {
                val heightRatio = (litres / maxScaleLitres).coerceIn(0.05f, 1f)
                host.dpToPx(maxBarHeightDp * heightRatio)
            }

            val barColor = when {
                isZero -> emptyColor
                index == bestMonthIndex -> activeColor
                else -> normalColor
            }

            val barCard = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, barHeightPx, 1f).apply {
                    marginStart = horizontalMarginPx
                    marginEnd = horizontalMarginPx
                }
                radius = if (isZero) host.dpToPx(1f).toFloat() else cornerRadiusPx
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(barColor)
            }
            container.addView(barCard)
        }

        val yearBreakdown = root.findViewById<LinearLayout>(R.id.listYearBreakdown)
        yearBreakdown.removeAllViews()

        if (logs.isEmpty()) {
            yearBreakdown.addView(
                host.createBreakdownCard("#9CE3EF", "No drinks this year", null, "0 L", 0)
            )
            return
        }

        val grouped = logs.groupBy { it.drinkName }
            .values
            .sortedByDescending { group -> group.sumOf { it.creditedMl } }

        grouped.forEachIndexed { index, group ->
            val first = group.first()
            val litres = group.sumOf { it.creditedMl } / 1000.0
            yearBreakdown.addView(
                host.createBreakdownCard(
                    colorHex = first.colorHex,
                    title = first.drinkName,
                    subtitle = "${group.size} logs this year",
                    rightText = formatLitres(litres),
                    topMarginDp = if (index == 0) 0 else 10
                )
            )
        }
    }

    private fun formatLitres(litres: Double): String {
        return if (litres % 1.0 == 0.0) {
            "${litres.toInt()} L"
        } else {
            String.format(Locale.US, "%.1f L", litres)
        }
    }
}