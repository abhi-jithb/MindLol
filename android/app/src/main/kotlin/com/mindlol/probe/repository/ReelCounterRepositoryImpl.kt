package com.mindlol.probe.repository

/**
 * Thread-safe production implementation of [ReelCounterRepository] backed by a [KeyValueStore].
 *
 * Enforces date-aware daily counting resets, persisted daily limits, supported app toggles, and limit-reached evaluation.
 *
 * Privacy Notice: Persists ONLY numerical aggregate counts, dates, app toggles, and limit preferences.
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
        com.mindlol.probe.StatusBroadcaster.notifyStatusChanged()
        return next
    }

    @Synchronized
    override fun getDailyLimit(): Int {
        return store.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
    }

    @Synchronized
    override fun setDailyLimit(limit: Int) {
        val validLimit = when {
            limit < 1 -> DEFAULT_DAILY_LIMIT
            limit > MAX_DAILY_LIMIT -> MAX_DAILY_LIMIT
            else -> limit
        }
        store.putInt(KEY_DAILY_LIMIT, validLimit)
        com.mindlol.probe.StatusBroadcaster.notifyStatusChanged()
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
            com.mindlol.probe.StatusBroadcaster.notifyStatusChanged()
            return true
        }
        return false
    }

    @Synchronized
    override fun resetTodayCount() {
        store.putInt(KEY_TODAY_COUNT, 0)
        com.mindlol.probe.StatusBroadcaster.notifyStatusChanged()
    }

    @Synchronized
    override fun isInstagramEnabled(): Boolean {
        return store.getBoolean(KEY_INSTAGRAM_ENABLED, true)
    }

    @Synchronized
    override fun setInstagramEnabled(enabled: Boolean) {
        store.putBoolean(KEY_INSTAGRAM_ENABLED, enabled)
        com.mindlol.probe.StatusBroadcaster.notifyStatusChanged()
    }

    @Synchronized
    override fun isYouTubeEnabled(): Boolean {
        return store.getBoolean(KEY_YOUTUBE_ENABLED, true)
    }

    @Synchronized
    override fun setYouTubeEnabled(enabled: Boolean) {
        store.putBoolean(KEY_YOUTUBE_ENABLED, enabled)
        com.mindlol.probe.StatusBroadcaster.notifyStatusChanged()
    }

    override fun getBlockingMode(): String {
        return BLOCKING_MODE_AT_LIMIT
    }

    companion object {
        const val DEFAULT_DAILY_LIMIT = 20
        const val MAX_DAILY_LIMIT = 500
        const val BLOCKING_MODE_AT_LIMIT = "BLOCK_AT_LIMIT"
        const val KEY_TODAY_COUNT = "today_count"
        const val KEY_DAILY_LIMIT = "daily_limit"
        const val KEY_LAST_ACTIVE_DATE = "last_active_date"
        const val KEY_INSTAGRAM_ENABLED = "instagram_enabled"
        const val KEY_YOUTUBE_ENABLED = "youtube_enabled"
    }
}
