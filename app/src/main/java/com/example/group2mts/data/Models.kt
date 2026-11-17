package com.example.group2mts.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val gender: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("weight_kg") val weightKg: Double = 60.0,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("activity_level") val activityLevel: String = "moderate",
    @SerialName("activity_source") val activitySource: String = "manual",
    @SerialName("auto_recalculate") val autoRecalculate: Boolean = true,
    @SerialName("custom_goal_ml") val customGoalMl: Int = 2000,
    @SerialName("unit_preference") val unitPreference: String = "ml",
    @SerialName("reminder_enabled") val reminderEnabled: Boolean = true
)

@Serializable
data class Beverage(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String,
    val subtitle: String = "",
    val category: String = "Custom",
    @SerialName("hydration_factor_pct") val hydrationFactorPct: Int = 100,
    @SerialName("default_amount_ml") val defaultAmountMl: Int = 250,
    @SerialName("icon_key") val iconKey: String = "water",
    @SerialName("color_hex") val colorHex: String = "#1FB6D5",
    @SerialName("is_default") val isDefault: Boolean = false
)

@Serializable
data class DailyTarget(
    @SerialName("user_id") val userId: String,
    @SerialName("target_date") val targetDate: String,
    @SerialName("base_ml") val baseMl: Int,
    @SerialName("activity_level") val activityLevel: String,
    @SerialName("step_count") val stepCount: Int = 0,
    @SerialName("activity_addon_ml") val activityAddonMl: Int,
    @SerialName("environment_type") val environmentType: String = "outdoor",
    @SerialName("weather_temp_c") val weatherTempC: Double? = null,
    @SerialName("weather_humidity_pct") val weatherHumidityPct: Int? = null,
    @SerialName("weather_condition") val weatherCondition: String? = null,
    @SerialName("environment_addon_ml") val environmentAddonMl: Int,
    @SerialName("total_goal_ml") val totalGoalMl: Int
)

@Serializable
data class DrinkLog(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("beverage_id") val beverageId: String? = null,
    @SerialName("drink_name") val drinkName: String = "Water",
    val category: String = "Water",
    @SerialName("color_hex") val colorHex: String = "#1FB6D5",
    @SerialName("consumed_ml") val consumedMl: Int,
    @SerialName("hydration_factor_pct") val hydrationFactorPct: Int = 100,
    @SerialName("credited_ml") val creditedMl: Int,
    @SerialName("logged_date") val loggedDate: String? = null,
    @SerialName("logged_at") val loggedAt: String? = null
)