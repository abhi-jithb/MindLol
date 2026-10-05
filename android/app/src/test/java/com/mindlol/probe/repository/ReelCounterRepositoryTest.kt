package com.mindlol.probe.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReelCounterRepositoryTest {

    private lateinit var store: KeyValueStore
    private lateinit var repository: ReelCounterRepositoryImpl

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
        repository = ReelCounterRepositoryImpl(store)
    }

    @Test
    fun `incrementCount increases count on same day`() {
        val dateToday = "2026-10-05"
        repository.checkAndResetDailyDate(dateToday)

        assertEquals(0, repository.getTodayCount())
        assertEquals(1, repository.incrementCount())
        assertEquals(2, repository.incrementCount())
        assertEquals(2, repository.getTodayCount())
    }

    @Test
    fun `checkAndResetDailyDate resets count on next day`() {
        val dateDay1 = "2026-10-05"
        val dateDay2 = "2026-10-06"

        repository.checkAndResetDailyDate(dateDay1)
        repository.incrementCount()
        repository.incrementCount()
        assertEquals(2, repository.getTodayCount())

        // Rollover to next day
        val resetOccurred = repository.checkAndResetDailyDate(dateDay2)
        assertTrue(resetOccurred)
        assertEquals(0, repository.getTodayCount())
    }

    @Test
    fun `checkAndResetDailyDate resets count multiple days later`() {
        val dateDay1 = "2026-10-05"
        val dateDayMulti = "2026-10-10"

        repository.checkAndResetDailyDate(dateDay1)
        repository.incrementCount()
        assertEquals(1, repository.getTodayCount())

        val resetOccurred = repository.checkAndResetDailyDate(dateDayMulti)
        assertTrue(resetOccurred)
        assertEquals(0, repository.getTodayCount())
    }

    @Test
    fun `setDailyLimit clamps limit between 1 and 500`() {
        repository.setDailyLimit(30)
        assertEquals(30, repository.getDailyLimit())

        // Rejects 0 -> falls back to default 20
        repository.setDailyLimit(0)
        assertEquals(20, repository.getDailyLimit())

        // Rejects negative -> falls back to default 20
        repository.setDailyLimit(-10)
        assertEquals(20, repository.getDailyLimit())

        // Clamps above max 500 -> 500
        repository.setDailyLimit(1000)
        assertEquals(500, repository.getDailyLimit())
    }

    @Test
    fun `supported app configuration toggles persist correctly`() {
        assertTrue(repository.isInstagramEnabled())
        assertTrue(repository.isYouTubeEnabled())

        repository.setInstagramEnabled(false)
        assertFalse(repository.isInstagramEnabled())
        assertTrue(repository.isYouTubeEnabled())

        repository.setYouTubeEnabled(false)
        assertFalse(repository.isYouTubeEnabled())
    }

    @Test
    fun `getBlockingMode returns BLOCK_AT_LIMIT`() {
        assertEquals("BLOCK_AT_LIMIT", repository.getBlockingMode())
    }
}
