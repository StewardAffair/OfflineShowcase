package ua.ofkkt.offlineshowcase

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ua.ofkkt.offlineshowcase.data.AppDatabase
import ua.ofkkt.offlineshowcase.data.Clock
import ua.ofkkt.offlineshowcase.data.FakeApi
import ua.ofkkt.offlineshowcase.data.ProductRepository
import ua.ofkkt.offlineshowcase.data.RefreshResult
import ua.ofkkt.offlineshowcase.data.SettingsStore
import ua.ofkkt.offlineshowcase.domain.ConflictStrategy
import java.io.File

/** Слайди 10–13: Repository + Room + «сервер»: offline-first, graceful degradation, конфлікти. */
@RunWith(AndroidJUnit4::class)
class RepositoryOfflineFirstTest {

    private lateinit var db: AppDatabase
    private lateinit var api: FakeApi
    private lateinit var repo: ProductRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        val clock = Clock()
        api = FakeApi(clock, latencyMs = 0)
        file = File.createTempFile("settings", ".preferences_pb", context.cacheDir)
        file.delete()
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        repo = ProductRepository(db, api, SettingsStore(store), clock)
    }

    @After
    fun tearDown() {
        db.close()
        file.delete()
    }

    @Test
    fun refreshFillsRoomAndFlow() = runBlocking {
        val result = repo.refresh()
        assertTrue(result is RefreshResult.Success)
        assertEquals(6, repo.getProducts().first().size)
    }

    @Test
    fun offlineRefreshKeepsCachedData() = runBlocking {
        repo.refresh()
        api.setOnline(false)
        val result = repo.refresh()
        assertTrue(result is RefreshResult.Failed)
        assertTrue(repo.isOffline.value)
        assertEquals(6, repo.getProducts().first().size)

        api.setOnline(true)
        repo.refresh()
        assertFalse(repo.isOffline.value)
    }

    @Test
    fun serverWinsOverwritesLocalEdit() = runBlocking {
        repo.refresh()
        repo.updatePriceLocally(1, 99.0)
        api.serverEditPrice(1, 77.0)
        repo.refresh(ConflictStrategy.ServerWins)
        assertEquals(77.0, repo.getProducts().first().first { it.id == 1 }.price, 0.0)
    }

    @Test
    fun timestampKeepsNewerLocalEditAndPushesIt() = runBlocking {
        repo.refresh()
        api.serverEditPrice(1, 77.0)
        Thread.sleep(5)
        repo.updatePriceLocally(1, 99.0)
        repo.refresh(ConflictStrategy.Timestamp)
        assertEquals(99.0, repo.getProducts().first().first { it.id == 1 }.price, 0.0)
        assertEquals(99.0, api.server.value.first { it.id == 1 }.price, 0.0)
    }

    @Test
    fun shouldRefreshOnlyWhenCacheIsStale() = runBlocking {
        assertTrue(repo.shouldRefresh())
        repo.refresh()
        assertFalse(repo.shouldRefresh())
    }
}
