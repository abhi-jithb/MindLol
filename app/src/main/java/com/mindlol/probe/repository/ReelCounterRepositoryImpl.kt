package com.mindlol.probe.repository

/**
 * Thread-safe production implementation of [ReelCounterRepository] backed by a [KeyValueStore].
 *
 * Enforces date-aware daily counting resets, persisted daily limits, and limit-reached evaluation.
 *
 * Privacy Notice: Persists ONLY numerical aggregate counts, dates, and limit preferences.
 * Never stores usernames, captions, URLs, screenshots, or video content.
 */
class ReelCounterRepositoryImpl(
    private val store: KeyValueStore
) : ReelCounterRepository {

    @Synchronized
    override fun getTodayCount(): Int {
        checkAndResetDailyDate()
        return store.getInt(KEY_TODAY_COUNT, 0)
    }

    @Synchronized
    override fun incrementCount(): Int {
        checkAndResetDailyDate()
        val current = store.getInt(KEY_TODAY_COUNT, 0)
        val next = current + 1
        store.putInt(KEY_TODAY_COUNT, next)
        return next
    }

    @Synchronized
    override fun getDailyLimit(): Int {
        return store.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
    }

    @Synchronized
    override fun setDailyLimit(limit: Int) {
        val validLimit = if (limit < 1) DEFAULT_DAILY_LIMIT else limit
        store.putInt(KEY_DAILY_LIMIT, validLimit)
    }

    @Synchronized
    override fun isLimitReached(): Boolean {
        return getTodayCount() >= getDailyLimit()
    }

    @Synchronized
    override fun checkAndResetDailyDate(currentDateStr: String): Boolean {
        val storedDate = store.getString(KEY_LAST_ACTIVE_DATE, null)
        if (storedDate == null || storedDate != currentDateStr) {
            store.putInt(KEY_TODAY_COUNT, 0)
            store.putString(KEY_LAST_ACTIVE_DATE, currentDateStr)
            return true
        }
        return false
    }

    @Synchronized
    override fun resetTodayCount() {
        store.putInt(KEY_TODAY_COUNT, 0)
    }

    companion object {
        const val DEFAULT_DAILY_LIMIT = 20
        const val KEY_TODAY_COUNT = "today_count"
        const val KEY_DAILY_LIMIT = "daily_limit"
        const val KEY_LAST_ACTIVE_DATE = "last_active_date"
    }
}
