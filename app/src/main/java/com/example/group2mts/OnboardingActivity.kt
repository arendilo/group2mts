package com.example.group2mts

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.HydrationRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ActivityLevel(val label: String, val dbValue: String, val addonMl: Int) {
    Light("Light", "low", 0),
    Medium("Medium", "moderate", 400),
    Heavy("Heavy", "high", 650)
}

class OnboardingActivity : AppCompatActivity() {

    private lateinit var pMonth: WheelView
    private lateinit var pDay: WheelView
    private lateinit var pYear: WheelView
    private lateinit var pWeight: WheelView
    private lateinit var pHeight: WheelView
    private lateinit var tvWeightLabel: TextView
    private lateinit var tvHeightLabel: TextView
    private lateinit var tvActivity: TextView
    private lateinit var tvGoal: TextView
    private lateinit var btnMinus: MaterialButton
    private lateinit var btnPlus: MaterialButton
    private lateinit var cardMale: MaterialCardView
    private lateinit var cardFemale: MaterialCardView
    private lateinit var tvMaleIcon: TextView
    private lateinit var tvFemaleIcon: TextView
    private lateinit var cardUnitMetric: MaterialCardView
    private lateinit var cardUnitImperial: MaterialCardView
    private lateinit var cardGoalRecommended: MaterialCardView
    private lateinit var cardGoalCustom: MaterialCardView

    private val years = (1930..LocalDate.now().year).toList()
    private var weightVals = emptyList<Double>()
    private var heightVals = emptyList<Double>()

    private var isMetric = true
    private var gender = "female"
    private var level = ActivityLevel.Medium
    private var customMode = false
    private var goalMl = 2000

    private val repo = HydrationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        pMonth = findViewById(R.id.pickerMonth)
        pDay = findViewById(R.id.pickerDay)
        pYear = findViewById(R.id.pickerYear)
        pWeight = findViewById(R.id.pickerWeight)
        pHeight = findViewById(R.id.pickerHeight)
        tvWeightLabel = findViewById(R.id.tvWeightLabel)
        tvHeightLabel = findViewById(R.id.tvHeightLabel)
        tvActivity = findViewById(R.id.tvActivity)
        tvGoal = findViewById(R.id.tvGoal)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        cardMale = findViewById(R.id.cardMale)
        cardFemale = findViewById(R.id.cardFemale)
        tvMaleIcon = findViewById(R.id.tvMaleIcon)
        tvFemaleIcon = findViewById(R.id.tvFemaleIcon)
        cardUnitMetric = findViewById(R.id.cardUnitMetric)
        cardUnitImperial = findViewById(R.id.cardUnitImperial)
        cardGoalRecommended = findViewById(R.id.cardGoalRecommended)
        cardGoalCustom = findViewById(R.id.cardGoalCustom)

        setupDob()
        applyUnits(58.0, 165.0)
        setGender("female")
        tvActivity.text = level.label
        setUnits(true)
        setGoalMode(false)

        pWeight.onSelected = { updateGoal() }

        tvGoal.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitTypedGoal()
                tvGoal.clearFocus()
                true
            } else {
                false
            }
        }
        tvGoal.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) commitTypedGoal()
        }

        cardMale.setOnClickListener { setGender("male") }
        cardFemale.setOnClickListener { setGender("female") }

        cardUnitMetric.setOnClickListener { setUnits(true) }
        cardUnitImperial.setOnClickListener { setUnits(false) }

        cardGoalRecommended.setOnClickListener { setGoalMode(false) }
        cardGoalCustom.setOnClickListener { setGoalMode(true) }

        btnMinus.setOnClickListener {
            goalMl = (goalMl - 100).coerceAtLeast(1000)
            updateGoal()
        }
        btnPlus.setOnClickListener {
            goalMl = (goalMl + 100).coerceAtMost(6000)
            updateGoal()
        }

        findViewById<View>(R.id.rowActivity).setOnClickListener { showActivitySheet() }
        findViewById<MaterialButton>(R.id.btnContinue).setOnClickListener { saveAndContinue() }
    }

    // Date of birth

    private fun setupDob() {
        pMonth.setItems((1..12).map { two(it) }, 9)
        pYear.setItems(years.map { it.toString() }, years.indexOf(2000).coerceAtLeast(0))
        refreshDays(16)
        pMonth.onSelected = { refreshDays(pDay.currentIndex()) }
        pYear.onSelected = { refreshDays(pDay.currentIndex()) }
    }

    private fun refreshDays(keepIndex: Int) {
        val year = years[pYear.currentIndex().coerceIn(0, years.size - 1)]
        val month = pMonth.currentIndex().coerceIn(0, 11) + 1
        val days = YearMonth.of(year, month).lengthOfMonth()
        pDay.setItems((1..days).map { two(it) }, minOf(keepIndex, days - 1))
    }

    private fun two(n: Int) = String.format(Locale.US, "%02d", n)

    // Weight and height (whole numbers)

    private fun range(min: Double, max: Double, step: Double): List<Double> {
        val n = ((max - min) / step).roundToInt()
        return List(n + 1) { min + it * step }
    }

    private fun nearest(list: List<Double>, v: Double): Int =
        list.indices.minByOrNull { abs(list[it] - v) } ?: 0

    private fun applyUnits(weightKg: Double, heightCm: Double) {
        weightVals = if (isMetric) range(30.0, 200.0, 1.0) else range(66.0, 440.0, 1.0)
        heightVals = if (isMetric) range(100.0, 230.0, 1.0) else range(40.0, 90.0, 1.0)
        val w = if (isMetric) weightKg else weightKg * 2.20462
        val h = if (isMetric) heightCm else heightCm / 2.54
        pWeight.setItems(weightVals.map { it.roundToInt().toString() }, nearest(weightVals, w))
        pHeight.setItems(heightVals.map { heightLabel(it) }, nearest(heightVals, h))
        tvWeightLabel.text = if (isMetric) "Weight (kg)" else "Weight (lbs)"
        tvHeightLabel.text = if (isMetric) "Height (cm)" else "Height (ft)"
    }

    private fun heightLabel(v: Double): String {
        val n = v.roundToInt()
        return if (isMetric) n.toString() else "${n / 12}'${n % 12}\""
    }

    private fun weightKg(): Double {
        val v = weightVals[pWeight.currentIndex().coerceIn(0, weightVals.size - 1)]
        return if (isMetric) v else v / 2.20462
    }

    private fun heightCm(): Double {
        val v = heightVals[pHeight.currentIndex().coerceIn(0, heightVals.size - 1)]
        return if (isMetric) v else v * 2.54
    }

    private fun setUnits(metric: Boolean) {
        if (metric != isMetric) {
            val kg = weightKg()
            val cm = heightCm()
            isMetric = metric
            applyUnits(kg, cm)
            updateGoal()
        }
        setPill(cardUnitMetric, isMetric)
        setPill(cardUnitImperial, !isMetric)
    }

    // Gender

    private fun setGender(g: String) {
        gender = g
        styleSelected(cardMale, g == "male")
        styleSelected(cardFemale, g == "female")
        tvMaleIcon.backgroundTintList = ColorStateList.valueOf(
            color(if (g == "male") R.color.wave_blue else R.color.on_background)
        )
        tvFemaleIcon.backgroundTintList = ColorStateList.valueOf(
            color(if (g == "female") R.color.wave_blue else R.color.on_background)
        )
    }

    // Activity level

    private fun showActivitySheet() {
        val dialog = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.sheet_activity_level, null)
        var pending = level
        val cards = mapOf(
            ActivityLevel.Light to v.findViewById<MaterialCardView>(R.id.cardLight),
            ActivityLevel.Medium to v.findViewById<MaterialCardView>(R.id.cardMedium),
            ActivityLevel.Heavy to v.findViewById<MaterialCardView>(R.id.cardHeavy)
        )
        fun refresh() = cards.forEach { (lvl, card) -> styleSelected(card, lvl == pending) }
        cards.forEach { (lvl, card) ->
            card.setOnClickListener {
                pending = lvl
                refresh()
            }
        }
        refresh()
        v.findViewById<View>(R.id.btnSheetClose).setOnClickListener { dialog.dismiss() }
        v.findViewById<MaterialButton>(R.id.btnSheetContinue).setOnClickListener {
            level = pending
            tvActivity.text = level.label
            updateGoal()
            dialog.dismiss()
        }
        dialog.setContentView(v)
        dialog.show()
    }

    // Daily goal

    private fun recommendedMl(): Int {
        val raw = (weightKg() * 33).roundToInt() + level.addonMl
        return raw.coerceIn(1200, 6000)
    }

    private fun setGoalMode(custom: Boolean) {
        customMode = custom
        setPill(cardGoalRecommended, !custom)
        setPill(cardGoalCustom, custom)
        updateGoal()
    }

    private fun commitTypedGoal() {
        if (!customMode) return
        val typed = tvGoal.text.toString().replace(",", "").toIntOrNull()
        if (typed != null) goalMl = typed.coerceIn(1000, 6000)
        updateGoal()
    }

    private fun updateGoal() {
        if (!customMode) goalMl = recommendedMl()
        tvGoal.text = String.format(Locale.US, "%,d", goalMl)
        btnMinus.isEnabled = customMode
        btnPlus.isEnabled = customMode
        tvGoal.isEnabled = customMode
    }

    // Save and continue

    private fun saveAndContinue() {
        commitTypedGoal()

        val btn = findViewById<MaterialButton>(R.id.btnContinue)
        val year = years[pYear.currentIndex().coerceIn(0, years.size - 1)]
        val month = pMonth.currentIndex().coerceIn(0, 11) + 1
        val day = pDay.currentIndex() + 1

        val genderValue = gender
        val dobValue = LocalDate.of(year, month, day).toString()
        val weightValue = weightKg()
        val heightValue = heightCm()
        val activityValue = level.dbValue
        val autoValue = !customMode
        val goalValue = goalMl

        btn.isEnabled = false
        lifecycleScope.launch {
            try {
                repo.updateProfile {
                    set("gender", genderValue)
                    set("date_of_birth", dobValue)
                    set("weight_kg", weightValue)
                    set("height_cm", heightValue)
                    set("activity_level", activityValue)
                    set("auto_recalculate", autoValue)
                    set("custom_goal_ml", goalValue)
                }
                startActivity(Intent(this@OnboardingActivity, RemindersActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnboardingActivity,
                    e.message ?: "Failed to save",
                    Toast.LENGTH_LONG
                ).show()
                btn.isEnabled = true
            }
        }
    }

    // Styling helpers

    private fun setPill(card: MaterialCardView, selected: Boolean) {
        card.setCardBackgroundColor(color(if (selected) R.color.wave_blue else R.color.white))
        (card.getChildAt(0) as TextView).setTextColor(
            color(if (selected) R.color.white else R.color.muted)
        )
    }

    private fun styleSelected(card: MaterialCardView, selected: Boolean) {
        card.strokeColor = color(if (selected) R.color.wave_blue else R.color.tab_border)
        card.strokeWidth = dp(if (selected) 2 else 1)
        card.setCardBackgroundColor(
            color(if (selected) R.color.primary_container else R.color.surface)
        )
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}