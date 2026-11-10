package ua.ofkkt.offlineshowcase.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ua.ofkkt.offlineshowcase.domain.ConflictStrategy
import ua.ofkkt.offlineshowcase.domain.Product
import ua.ofkkt.offlineshowcase.domain.isConflict
import ua.ofkkt.offlineshowcase.domain.resolveConflict
import ua.ofkkt.offlineshowcase.domain.shouldRefresh as shouldRefreshByTime
import java.io.IOException

sealed interface RefreshResult {
    data class Success(val count: Int, val notes: List<String>) : RefreshResult
    data class Failed(val message: String) : RefreshResult
}

/**
 * Слайди 10, 11, 13, 15: єдина точка між мережею та Room.
 * UI читає ТІЛЬКИ з Room (getProducts), а refresh() лише оновлює локальну базу.
 */
class ProductRepository(
    private val db: AppDatabase,
    private val api: FakeApi,
    private val settings: SettingsStore,
    private val clock: Clock,
) {
    private val dao = db.productDao()

    private val _isOffline = MutableStateFlow(false)
    /** Слайд 13: прапорець для банера «Офлайн». */
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Єдине джерело правди: Flow із Room, перетворений Mapper-ом на доменну модель. */
    fun getProducts(): Flow<List<Product>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    val lastSync: Flow<Long> = settings.lastSync

    suspend fun shouldRefresh(): Boolean =
        shouldRefreshByTime(lastSync = settings.lastSync.first(), now = clock.now())

    /**
     * Слайди 11 і 13: мережа → Room; помилка мережі НЕ кидається вгору.
     * @param atomic true — deleteAll+insertAll в одній транзакції; false — «наївно», двома кроками
     *        (між кроками Flow може випустити порожній список — див. екран «Offline-First»).
     */
    suspend fun refresh(
        strategy: ConflictStrategy = ConflictStrategy.ServerWins,
        atomic: Boolean = true,
    ): RefreshResult {
        return try {
            val remote = api.getProducts()
            val now = clock.now()
            val notes = mutableListOf<String>()

            val merged = remote.map { remoteItem ->
                val local = dao.getById(remoteItem.id)?.toDomain()
                val resolution = resolveConflict(local, remoteItem, strategy)
                if (isConflict(local, remoteItem)) {
                    notes += if (resolution.localWon) {
                        "#${remoteItem.id}: виграла ЛОКАЛЬНА версія (${strategy.label}) — надсилаємо на сервер"
                    } else {
                        "#${remoteItem.id}: виграла СЕРВЕРНА версія (${strategy.label}) — локальна зміна втрачена"
                    }
                }
                if (resolution.localWon) api.putProduct(resolution.winner)
                resolution.winner.toEntity(cachedAt = now)
            }

            if (atomic) {
                db.withTransaction {
                    dao.deleteAll()
                    dao.insertAll(merged)
                }
            } else {
                dao.deleteAll()
                delay(NAIVE_GAP_MS) // у реальному коді паузи немає, але вікно між кроками все одно існує
                dao.insertAll(merged)
            }

            settings.saveLastSyncTime(now)
            _isOffline.value = false
            _lastError.value = null
            RefreshResult.Success(merged.size, notes)
        } catch (e: IOException) {
            // Слайд 13: логуємо, але не падаємо — локальні дані залишаються.
            _isOffline.value = true
            _lastError.value = e.message
            RefreshResult.Failed(e.message ?: "Мережева помилка")
        }
    }

    /** Локальна зміна (наприклад, користувач редагує ціну офлайн). */
    suspend fun updatePriceLocally(id: Int, price: Double) {
        val current = dao.getById(id) ?: return
        dao.update(current.copy(price = price, updatedAt = clock.now()))
    }

    suspend fun localCount(): Int = dao.count()

    /** Скидання демо: порожня локальна база й невідома дата синхронізації. */
    suspend fun resetLocal() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        settings.clear()
        _isOffline.value = false
        _lastError.value = null
    }

    private companion object {
        const val NAIVE_GAP_MS = 250L
    }
}
