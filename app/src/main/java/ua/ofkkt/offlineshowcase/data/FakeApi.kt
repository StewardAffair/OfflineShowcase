package ua.ofkkt.offlineshowcase.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ua.ofkkt.offlineshowcase.domain.Product
import java.io.IOException

/** Мережева помилка: так виглядає «немає інтернету» для Repository (слайд 13). */
class NoNetworkException(message: String = "Немає з'єднання з сервером") : IOException(message)

/**
 * «Сервер» у пам'яті. Замість Retrofit/Ktor з Лекції 17 — тут можна кнопкою вимкнути мережу,
 * змінити дані «з іншого пристрою» й порахувати запити.
 */
class FakeApi(private val clock: Clock, var latencyMs: Long = 400L) {

    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val _server = MutableStateFlow(seed())
    /** Що зараз лежить на сервері (щоб показати на екрані). */
    val server: StateFlow<List<Product>> = _server.asStateFlow()

    private val _requests = MutableStateFlow(0)
    val requests: StateFlow<Int> = _requests.asStateFlow()

    private var nextId = 100

    fun setOnline(value: Boolean) {
        _online.value = value
    }

    private suspend fun guard() {
        delay(latencyMs)
        if (!_online.value) throw NoNetworkException()
        _requests.update { it + 1 }
    }

    /** GET /products */
    suspend fun getProducts(): List<Product> {
        guard()
        return _server.value
    }

    /** PUT /products/{id} — клієнт надсилає свою версію (якщо вона виграла конфлікт). */
    suspend fun putProduct(product: Product) {
        guard()
        _server.update { list -> list.map { if (it.id == product.id) product else it } }
    }

    // ---- Дії «іншого пристрою» (не проходять через мережу клієнта) ----

    fun serverEditPrice(id: Int, price: Double) {
        _server.update { list ->
            list.map { if (it.id == id) it.copy(price = price, updatedAt = clock.now()) else it }
        }
    }

    fun serverAdd(name: String, price: Double) {
        val product = Product(nextId++, name, price, clock.now(), "Новий постачальник", "Київ")
        _server.update { it + product }
    }

    fun serverDelete(id: Int) {
        _server.update { list -> list.filterNot { it.id == id } }
    }

    fun reset() {
        _server.value = seed()
        _requests.value = 0
        nextId = 100
        _online.value = true
    }

    private fun seed(): List<Product> {
        val t = clock.now() - 60_000L
        return listOf(
            Product(1, "Яблука", 42.0, t, "ФОП Коваль", "Умань"),
            Product(2, "Молоко 2.5%", 38.5, t, "Ферма «Зоря»", "Полтава"),
            Product(3, "Хліб житній", 27.0, t, "Пекарня №3", "Одеса"),
            Product(4, "Сир кисломолочний", 64.0, t, "Ферма «Зоря»", "Полтава"),
            Product(5, "Кава мелена", 210.0, t, "Кав'ярня «Зерно»", "Львів"),
            Product(6, "Гречка", 55.0, t, "Агро-Південь", "Миколаїв"),
        )
    }
}
