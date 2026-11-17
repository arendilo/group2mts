package com.example.group2mts.data

import kotlin.math.roundToInt

object GoalCalculator {
    fun build(
        p: Profile,
        date: String,
        environmentType: String = "outdoor",
        tempC: Double? = null,
        humidityPct: Int? = null,
        condition: String? = null
    ): DailyTarget {
        val base = (p.weightKg * 33).roundToInt()
        val activityAddon = when (p.activityLevel) {
            "low" -> 0
            "high" -> 650
            else -> 400
        }
        val envAddon = when {
            environmentType == "indoor_ac" -> 400
            tempC != null && tempC > 30 && humidityPct != null && humidityPct > 70 -> 600
            tempC != null && tempC > 30 -> 400
            else -> 0
        }
        val total = if (p.autoRecalculate) base + activityAddon + envAddon else p.customGoalMl

        return DailyTarget(
            userId = p.id,
            targetDate = date,
            baseMl = base,
            activityLevel = p.activityLevel,
            activityAddonMl = activityAddon,
            environmentType = environmentType,
            weatherTempC = tempC,
            weatherHumidityPct = humidityPct,
            weatherCondition = condition,
            environmentAddonMl = envAddon,
            totalGoalMl = total
        )
    }
}