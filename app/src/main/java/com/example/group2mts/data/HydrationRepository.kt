package com.example.group2mts.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

class HydrationRepository {
    private val db = SupabaseProvider.client
    private val uid get() = db.auth.currentUserOrNull()?.id
        ?: error("Not signed in")

    // Profile
    suspend fun getProfile(): Profile =
        db.from("profiles").select { filter { eq("id", uid) } }.decodeSingle()

    suspend fun updateProfile(block: io.github.jan.supabase.postgrest.query.PostgrestUpdate.() -> Unit) =
        db.from("profiles").update(block) { filter { eq("id", uid) } }

    // Beverages
    suspend fun getBeverages(): List<Beverage> =
        db.from("beverages").select {
            filter { eq("user_id", uid) }
            order("created_at", Order.ASCENDING)
        }.decodeList()

    suspend fun addBeverage(b: Beverage) {
        db.from("beverages").insert(b.copy(userId = uid))
    }

    suspend fun deleteBeverage(id: String) {
        db.from("beverages").delete { filter { eq("id", id) } }
    }

    // Drink logs
    suspend fun logDrink(
        name: String, category: String, colorHex: String,
        consumedMl: Int, factorPct: Int, beverageId: String? = null,
        loggedDate: String = java.time.LocalDate.now().toString()   // tanggal lokal
    ) {
        val credited = (consumedMl * factorPct / 100.0).roundToInt()
        db.from("drink_logs").insert(
            DrinkLog(
                userId = uid, beverageId = beverageId, drinkName = name,
                category = category, colorHex = colorHex, consumedMl = consumedMl,
                hydrationFactorPct = factorPct, creditedMl = credited,
                loggedDate = loggedDate
            )
        )
    }

    suspend fun getLogsForDate(date: String): List<DrinkLog> =
        db.from("drink_logs").select {
            filter { eq("user_id", uid); eq("logged_date", date) }
            order("logged_at", Order.DESCENDING)
        }.decodeList()

    // Daily target
    suspend fun upsertTodayTarget(t: DailyTarget) {
        db.from("daily_hydration_targets").upsert(t) {
            onConflict = "user_id,target_date"
        }
    }

    suspend fun getTarget(date: String): DailyTarget? =
        db.from("daily_hydration_targets").select {
            filter { eq("user_id", uid); eq("target_date", date) }
        }.decodeSingleOrNull()

    suspend fun getLogsBetween(startDate: String, endDate: String): List<DrinkLog> =
        db.from("drink_logs").select {
            filter {
                eq("user_id", uid)
                gte("logged_date", startDate)
                lte("logged_date", endDate)
            }
            order("logged_at", Order.ASCENDING)
        }.decodeList()

    suspend fun getTargetsBetween(startDate: String, endDate: String): List<DailyTarget> =
        db.from("daily_hydration_targets").select {
            filter {
                eq("user_id", uid)
                gte("target_date", startDate)
                lte("target_date", endDate)
            }
            order("target_date", Order.ASCENDING)
        }.decodeList()

    // Streak RPC
    suspend fun getStreak(): Int =
        db.postgrest.rpc(
            "get_user_streak",
            buildJsonObject {
                put("p_user_id", uid)
                put("p_today", java.time.LocalDate.now().toString())
            }
        ).decodeAs<Int>()
}