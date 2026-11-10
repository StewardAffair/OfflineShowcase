package ua.ofkkt.offlineshowcase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ua.ofkkt.offlineshowcase.data.Clock
import ua.ofkkt.offlineshowcase.data.FakeApi
import ua.ofkkt.offlineshowcase.data.NoNetworkException

class FakeApiTest {

    private fun api() = FakeApi(Clock(), latencyMs = 0)

    @Test
    fun returnsSeedProductsOnline() = runTest {
        val api = api()
        assertEquals(6, api.getProducts().size)
        assertEquals(1, api.requests.value)
    }

    @Test
    fun throwsWhenOffline() = runTest {
        val api = api()
        api.setOnline(false)
        try {
            api.getProducts()
            fail("очікували NoNetworkException")
        } catch (e: NoNetworkException) {
            assertEquals(0, api.requests.value)
        }
    }

    @Test
    fun serverEditChangesPriceAndTimestamp() = runTest {
        val api = api()
        val before = api.getProducts().first { it.id == 1 }
        api.serverEditPrice(1, 77.0)
        val after = api.getProducts().first { it.id == 1 }
        assertEquals(77.0, after.price, 0.0)
        assertTrue(after.updatedAt > before.updatedAt)
    }

    @Test
    fun clockCanBeAdvanced() {
        val clock = Clock()
        val before = clock.now()
        clock.advance(31 * 60_000L)
        assertTrue(clock.now() - before >= 31 * 60_000L)
    }

    @Test
    fun resetRestoresSeed() = runTest {
        val api = api()
        api.serverAdd("Новинка", 1.0)
        assertEquals(7, api.getProducts().size)
        api.reset()
        assertEquals(6, api.getProducts().size)
    }
}
