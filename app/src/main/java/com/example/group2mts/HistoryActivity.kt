package com.example.group2mts

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.example.group2mts.data.HydrationRepository
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.YearMonth

class HistoryActivity : AppCompatActivity() {

    val repo = HydrationRepository()
    var selectedDate: LocalDate = LocalDate.now()
    var selectedMonth: YearMonth = YearMonth.from(selectedDate)
    var selectedYear: Int = selectedDate.year
    var fallbackGoalMl: Int = 2000

    private var activePeriod: String = "Day"
    private var isFirstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        val navBar = findViewById<View>(R.id.navBar)
        val navBarBasePadding = navBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            navBar.updatePadding(bottom = navBarBasePadding + systemBars.bottom)
            insets
        }

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

        if (savedInstanceState == null) {
            switchPeriod("Day")
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        when (activePeriod) {
            "Day" -> (supportFragmentManager.findFragmentByTag("Day") as? HistoryDayFragment)?.loadDayData()
            "Month" -> (supportFragmentManager.findFragmentByTag("Month") as? HistoryMonthFragment)?.loadMonthData()
            "Year" -> (supportFragmentManager.findFragmentByTag("Year") as? HistoryYearFragment)?.loadYearData()
        }
    }

    fun switchPeriod(mode: String) {
        activePeriod = mode
        val tags = listOf("Day", "Month", "Year")
        val transaction = supportFragmentManager.beginTransaction()

        tags.forEach { tag ->
            val existing = supportFragmentManager.findFragmentByTag(tag)
            if (tag == mode) {
                if (existing == null) {
                    val fragment: Fragment = when (tag) {
                        "Month" -> HistoryMonthFragment()
                        "Year" -> HistoryYearFragment()
                        else -> HistoryDayFragment()
                    }
                    transaction.add(R.id.historyFragmentContainer, fragment, tag)
                } else {
                    transaction.show(existing)
                }
            } else if (existing != null) {
                transaction.hide(existing)
            }
        }

        transaction.commit()
        findViewById<NestedScrollView>(R.id.scrollHistory).scrollTo(0, 0)
    }

    fun createBreakdownCard(
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

    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}