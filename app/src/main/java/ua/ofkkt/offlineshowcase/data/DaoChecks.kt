package ua.ofkkt.offlineshowcase.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.first

data class CheckResult(val name: String, val passed: Boolean, val message: String)

/**
 * Слайд 18: тести DAO на in-memory базі. Ті самі перевірки запускаються
 * і кнопкою в застосунку, і в androidTest (DaoInMemoryTest).
 */
object DaoChecks {

    suspend fun runAll(context: Context): List<CheckResult> = listOf(
        check("insertAndGetAll (слайд 18)", context) { dao ->
            dao.insertAll(listOf(ProductEntity(id = 1, name = "Test", price = 9.99)))
            val result = dao.getAll().first()
            require(result.size == 1) { "очікували 1 рядок, отримали ${result.size}" }
            require(result[0].name == "Test") { "очікували Test, отримали ${result[0].name}" }
        },
        check("ORDER BY product_name ASC", context) { dao ->
            dao.insertAll(listOf(ProductEntity(1, "Борщ", 1.0), ProductEntity(2, "Аджика", 2.0)))
            val names = dao.getAll().first().map { it.name }
            require(names == listOf("Аджика", "Борщ")) { "порядок: $names" }
        },
        check("REPLACE не створює дублікатів", context) { dao ->
            dao.insertAll(listOf(ProductEntity(1, "Старий", 1.0)))
            dao.insertAll(listOf(ProductEntity(1, "Новий", 2.0)))
            val rows = dao.getAll().first()
            require(rows.size == 1 && rows[0].name == "Новий") { "рядки: ${rows.map { it.name }}" }
        },
        check("ABORT кидає SQLiteConstraintException", context) { dao ->
            dao.insertAbort(ProductEntity(1, "Перший", 1.0))
            var thrown = false
            try {
                dao.insertAbort(ProductEntity(1, "Дубль", 2.0))
            } catch (e: SQLiteConstraintException) {
                thrown = true
            }
            require(thrown) { "виняток не виник" }
        },
        check("Ізоляція: нова база порожня", context) { dao ->
            dao.insertAll(listOf(ProductEntity(1, "Зайвий", 1.0)))
            val other = AppDatabase.inMemory(context)
            try {
                require(other.productDao().count() == 0) { "друга база не порожня" }
            } finally {
                other.close()
            }
        },
    )

    private suspend fun check(
        name: String,
        context: Context,
        body: suspend (ProductDao) -> Unit,
    ): CheckResult {
        val db = AppDatabase.inMemory(context)
        return try {
            body(db.productDao())
            CheckResult(name, true, "OK")
        } catch (e: Throwable) {
            CheckResult(name, false, "${e::class.java.simpleName}: ${e.message}")
        } finally {
            db.close()
        }
    }
}
