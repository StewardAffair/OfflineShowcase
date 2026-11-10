package ua.ofkkt.offlineshowcase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.ofkkt.offlineshowcase.domain.CACHE_TIMEOUT_MS
import ua.ofkkt.offlineshowcase.domain.ConflictStrategy
import ua.ofkkt.offlineshowcase.domain.Product
import ua.ofkkt.offlineshowcase.domain.isConflict
import ua.ofkkt.offlineshowcase.domain.resolveConflict
import ua.ofkkt.offlineshowcase.domain.shouldRefresh

/** Слайди 11–12: чиста логіка синхронізації й конфліктів — тестується без Android. */
class SyncLogicTest {

    private fun product(price: Double, updatedAt: Long) = Product(1, "Яблука", price, updatedAt)

    @Test
    fun freshCacheIsNotRefreshed() {
        assertFalse(shouldRefresh(lastSync = 1_000, now = 1_000 + CACHE_TIMEOUT_MS))
    }

    @Test
    fun staleCacheIsRefreshed() {
        assertTrue(shouldRefresh(lastSync = 1_000, now = 1_001 + CACHE_TIMEOUT_MS))
    }

    @Test
    fun neverSyncedCacheIsRefreshed() {
        assertTrue(shouldRefresh(lastSync = 0, now = System.currentTimeMillis()))
    }

    @Test
    fun serverWinsAlwaysTakesRemote() {
        val local = product(99.0, updatedAt = 2_000)
        val remote = product(77.0, updatedAt = 1_000)
        val result = resolveConflict(local, remote, ConflictStrategy.ServerWins)
        assertEquals(77.0, result.winner.price, 0.0)
        assertFalse(result.localWon)
    }

    @Test
    fun timestampKeepsNewerLocal() {
        val local = product(99.0, updatedAt = 2_000)
        val remote = product(77.0, updatedAt = 1_000)
        val result = resolveConflict(local, remote, ConflictStrategy.Timestamp)
        assertEquals(99.0, result.winner.price, 0.0)
        assertTrue(result.localWon)
    }

    @Test
    fun timestampTakesNewerRemote() {
        val local = product(99.0, updatedAt = 1_000)
        val remote = product(77.0, updatedAt = 2_000)
        val result = resolveConflict(local, remote, ConflictStrategy.Timestamp)
        assertEquals(77.0, result.winner.price, 0.0)
        assertFalse(result.localWon)
    }

    @Test
    fun equalTimestampsPreferServer() {
        val result = resolveConflict(product(99.0, 1_000), product(77.0, 1_000), ConflictStrategy.Timestamp)
        assertEquals(77.0, result.winner.price, 0.0)
    }

    @Test
    fun missingLocalTakesRemote() {
        val result = resolveConflict(null, product(77.0, 1_000), ConflictStrategy.Timestamp)
        assertEquals(77.0, result.winner.price, 0.0)
        assertFalse(result.localWon)
    }

    @Test
    fun conflictDetection() {
        assertFalse(isConflict(null, product(1.0, 1)))
        assertFalse(isConflict(product(1.0, 1), product(1.0, 2)))
        assertTrue(isConflict(product(1.0, 1), product(2.0, 2)))
    }
}
