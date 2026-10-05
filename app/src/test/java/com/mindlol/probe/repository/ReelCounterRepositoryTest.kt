package com.mindlol.probe.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class ReelCounterRepositoryTest {

    private lateinit var store: KeyValueStore
    private lateinit var repository: ReelCounterRepositoryImpl

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
        repository = ReelCounterRepositoryImpl(store)
    }

    @Test
    fun `test 1 - Initial repository state defaults to count 0 and default limit`() {
        assertEquals(0, repository.getTodayCount())
        assertEquals(ReelCounterRepositoryImpl.DEFAULT_DAILY_LIMIT, repository.getDailyLimit())
        assertFalse(repository.isLimitReached())
    }

    @Test
    fun `test 2 - Incrementing count updates and persists value`() {
        assertEquals(1, repository.incrementCount())
        assertEquals(2, repository.incrementCount())
        assertEquals(3, repository.incrementCount())
        assertEquals(3, repository.getTodayCount())
    }

    @Test
    fun `test 3 - Count survives repository re-instantiation simulating process restart`() {
        // Step 1: Save count = 5 on first instance
        repeat(5) { repository.incrementCount() }
        assertEquals(5, repository.getTodayCount())

        // Step 2: Create a new repository instance connected to same storage
        val restartedRepository = ReelCounterRepositoryImpl(store)

        // Step 3: Count must still be 5
        assertEquals(5, restartedRepository.getTodayCount())
    }

    @Test
    fun `test 4 - Date rollover resets count to 0 when date changes`() {
        val today = "2026-10-05"
        val nextDay = "2026-10-06"

        // Initialize today date and increment count to 10
        repository.checkAndResetDailyDate(today)
        repeat(10) { repository.incrementCount() }
        assertEquals(10, repository.getTodayCount())

        // Simulate next day access
        val wasReset = repository.checkAndResetDailyDate(nextDay)

        assertTrue(wasReset)
        assertEquals(0, repository.getTodayCount())
    }

    @Test
    fun `test 5 - Daily limit persistence and modification survive restart`() {
        repository.setDailyLimit(15)
        assertEquals(15, repository.getDailyLimit())

        // Re-instantiate repository
        val restartedRepo = ReelCounterRepositoryImpl(store)
        assertEquals(15, restartedRepo.getDailyLimit())
    }

    @Test
    fun `test 6 - Limit reached status correctly evaluates count vs daily limit`() {
        repository.setDailyLimit(3)
        assertFalse(repository.isLimitReached())

        repository.incrementCount() // 1
        repository.incrementCount() // 2
        assertFalse(repository.isLimitReached())

        repository.incrementCount() // 3 (equals limit)
        assertTrue(repository.isLimitReached())

        repository.incrementCount() // 4 (exceeds limit)
        assertTrue(repository.isLimitReached())
    }

    @Test
    fun `test 7 - Manual reset clears today count`() {
        repeat(7) { repository.incrementCount() }
        assertEquals(7, repository.getTodayCount())

        repository.resetTodayCount()
        assertEquals(0, repository.getTodayCount())
    }

    @Test
    fun `test 8 - Concurrent multithreaded increments are state-safe without data loss`() {
        val threadCount = 10
        val incrementsPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        for (i in 0 until threadCount) {
            executor.execute {
                try {
                    repeat(incrementsPerThread) {
                        repository.incrementCount()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertEquals(threadCount * incrementsPerThread, repository.getTodayCount())
    }

    @Test
    fun `test 9 - Setting daily limit below current count immediately triggers limit reached`() {
        repository.setDailyLimit(10)
        repeat(5) { repository.incrementCount() }
        assertFalse(repository.isLimitReached())

        // Reduce limit below current count
        repository.setDailyLimit(3)
        assertTrue(repository.isLimitReached())
    }

    @Test
    fun `test 10 - Setting invalid limit less than 1 falls back to DEFAULT_DAILY_LIMIT`() {
        repository.setDailyLimit(0)
        assertEquals(ReelCounterRepositoryImpl.DEFAULT_DAILY_LIMIT, repository.getDailyLimit())

        repository.setDailyLimit(-10)
        assertEquals(ReelCounterRepositoryImpl.DEFAULT_DAILY_LIMIT, repository.getDailyLimit())
    }

    @Test
    fun `test 11 - Process restart at or above limit preserves limitReached state`() {
        repository.setDailyLimit(5)
        repeat(6) { repository.incrementCount() } // 6 >= 5
        assertTrue(repository.isLimitReached())

        // Re-instantiate repository
        val restartedRepo = ReelCounterRepositoryImpl(store)
        assertTrue(restartedRepo.isLimitReached())
        assertEquals(6, restartedRepo.getTodayCount())
    }
}
