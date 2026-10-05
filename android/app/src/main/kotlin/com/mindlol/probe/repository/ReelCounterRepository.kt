package com.mindlol.probe.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository interface defining Android application state management for Reel consumption counting and daily limits.
 */
interface ReelCounterRepository {
    fun getTodayCount(): Int
    fun incrementCount(): Int
    fun getDailyLimit(): Int
    fun setDailyLimit(limit: Int)
    fun isLimitReached(): Boolean
    fun checkAndResetDailyDate(currentDateStr: String = getCurrentDateString()): Boolean
    fun resetTodayCount()

    // Supported App Configurations (Phase 13)
    fun isInstagramEnabled(): Boolean
    fun setInstagramEnabled(enabled: Boolean)
    fun isYouTubeEnabled(): Boolean
    fun setYouTubeEnabled(enabled: Boolean)

    // Blocking Mode Abstraction (Phase 13)
    fun getBlockingMode(): String

    companion object {
        fun getCurrentDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}
