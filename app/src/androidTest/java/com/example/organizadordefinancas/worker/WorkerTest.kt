package com.example.organizadordefinancas.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for WorkManager workers.
 * Uses WorkManager's testing library to run workers synchronously.
 */
@RunWith(AndroidJUnit4::class)
class WorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ==================== BillGenerationWorker Tests ====================

    @Test
    fun billGenerationWorker_executesSuccessfully() {
        // Create worker using TestListenableWorkerBuilder
        val worker = TestListenableWorkerBuilder<BillGenerationWorker>(context).build()

        // Run worker
        val result = runBlocking {
            worker.doWork()
        }

        // Verify success or retry (not failure)
        assertTrue(
            "Worker should succeed or retry",
            result is ListenableWorker.Result.Success || result is ListenableWorker.Result.Retry
        )
    }

    @Test
    fun billGenerationWorker_hasCorrectTags() {
        assertEquals("bill_generation", BillGenerationWorker.TAG)
        assertEquals("BillGenerationWork", BillGenerationWorker.WORK_NAME)
    }

    @Test
    fun billGenerationWorker_hasCorrectNotificationId() {
        assertEquals(1001, BillGenerationWorker.NOTIFICATION_ID)
        assertEquals("bill_generation_channel", BillGenerationWorker.CHANNEL_ID)
    }

    // ==================== OverdueCheckWorker Tests ====================

    @Test
    fun overdueCheckWorker_executesSuccessfully() {
        // Create worker
        val worker = TestListenableWorkerBuilder<OverdueCheckWorker>(context).build()

        // Run worker
        val result = runBlocking {
            worker.doWork()
        }

        // Verify success or retry (not failure)
        assertTrue(
            "Worker should succeed or retry",
            result is ListenableWorker.Result.Success || result is ListenableWorker.Result.Retry
        )
    }

    @Test
    fun overdueCheckWorker_hasCorrectTags() {
        assertEquals("overdue_check", OverdueCheckWorker.TAG)
        assertEquals("OverdueCheckWork", OverdueCheckWorker.WORK_NAME)
    }

    @Test
    fun overdueCheckWorker_hasCorrectNotificationId() {
        assertEquals(1002, OverdueCheckWorker.NOTIFICATION_ID)
        assertEquals("overdue_bills_channel", OverdueCheckWorker.CHANNEL_ID)
    }

    // ==================== WorkManagerScheduler Tests ====================

    @Test
    fun workManagerScheduler_scheduleAllWork_doesNotThrow() {
        // Should not throw any exception
        try {
            WorkManagerScheduler.scheduleAllWork(context)
            assertTrue(true)
        } catch (e: Exception) {
            fail("scheduleAllWork should not throw: ${e.message}")
        }
    }

    @Test
    fun workManagerScheduler_cancelAllWork_doesNotThrow() {
        // Schedule first
        WorkManagerScheduler.scheduleAllWork(context)

        // Cancel should not throw
        try {
            WorkManagerScheduler.cancelAllWork(context)
            assertTrue(true)
        } catch (e: Exception) {
            fail("cancelAllWork should not throw: ${e.message}")
        }
    }

    @Test
    fun workManagerScheduler_runBillGenerationNow_doesNotThrow() {
        try {
            WorkManagerScheduler.runBillGenerationNow(context)
            assertTrue(true)
        } catch (e: Exception) {
            fail("runBillGenerationNow should not throw: ${e.message}")
        }
    }

    @Test
    fun workManagerScheduler_runOverdueCheckNow_doesNotThrow() {
        try {
            WorkManagerScheduler.runOverdueCheckNow(context)
            assertTrue(true)
        } catch (e: Exception) {
            fail("runOverdueCheckNow should not throw: ${e.message}")
        }
    }

    @Test
    fun workManagerScheduler_cancelWorkByTag_doesNotThrow() {
        // Schedule first
        WorkManagerScheduler.scheduleAllWork(context)

        // Cancel by tag
        try {
            WorkManagerScheduler.cancelWorkByTag(context, BillGenerationWorker.TAG)
            WorkManagerScheduler.cancelWorkByTag(context, OverdueCheckWorker.TAG)
            assertTrue(true)
        } catch (e: Exception) {
            fail("cancelWorkByTag should not throw: ${e.message}")
        }
    }
}

