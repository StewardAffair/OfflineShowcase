package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.ProductEntity
import ua.ofkkt.offlineshowcase.domain.Product
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.ChoiceChips
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

private enum class Conflict(val title: String) { Replace("REPLACE"), Ignore("IGNORE"), Abort("ABORT") }

/** Слайд 6 (і слайд 16 про OnConflictStrategy): DAO — запити до даних. */
@Composable
fun DaoDemo() {
    val db = rememberScratchDb()
    val dao = db.productDao()
    val scope = rememberCoroutineScope()

    val flow = remember(dao) { dao.getAll() }
    val rows by flow.collectAsStateWithLifecycle(initialValue = emptyList<ProductEntity>())

    var strategyName by rememberSaveable { mutableStateOf(Conflict.Replace.name) }
    val strategy = Conflict.valueOf(strategyName)
    var result by remember { mutableStateOf("") }

    DemoSection("DAO з лекції (слайд 6)", note = "Файл data/Daos.kt. Реалізацію генерує Room під час компіляції.") {
        CodeSnippet(
            """
            @Dao
            interface ProductDao {
                @Query("SELECT * FROM products ORDER BY product_name ASC")
                fun getAll(): Flow<List<ProductEntity>>

                @Query("SELECT * FROM products WHERE id = :productId")
                suspend fun getById(productId: Int): ProductEntity?

                @Insert(onConflict = OnConflictStrategy.REPLACE)
                suspend fun insertAll(products: List<ProductEntity>)

                @Update suspend fun update(product: ProductEntity)
                @Delete suspend fun delete(product: ProductEntity)

                @Query("DELETE FROM products")
                suspend fun deleteAll()
            }
            """,
        )
        Tip("Помилка в SQL усередині @Query — це помилка збірки, а не краш під час виконання.", TipKind.Success)
    }

    DemoSection("Таблиця products (in-memory база цього екрана)") {
        ProductRowsFromEntities(rows)
        ActionRow(
            listOf(
                "Заповнити #1–3" to { scope.launch { dao.deleteAll(); dao.insertAll(sampleEntities()) } },
                "@Update: #2 ціна +10" to {
                    scope.launch { dao.getById(2)?.let { dao.update(it.copy(price = it.price + 10)) } }
                },
                "@Delete: видалити #3" to { scope.launch { dao.getById(3)?.let { dao.delete(it) } } },
                "deleteAll()" to { scope.launch { dao.deleteAll() } },
            ),
            primaryFirst = true,
        )
    }

    DemoSection(
        "OnConflictStrategy: вставка рядка, що вже існує",
        note = "Спробуємо вставити id = 1 з іншими даними («Дубль», 999 ₴). Спершу натисніть «Заповнити».",
    ) {
        ChoiceChips(Conflict.entries, strategy, { strategyName = it.name }, { it.title })
        ActionRow(
            listOf(
                "Вставити id=1 «Дубль»" to {
                    scope.launch {
                        val duplicate = ProductEntity(1, "Дубль", 999.0)
                        result = try {
                            val rowId = when (strategy) {
                                Conflict.Replace -> dao.insertReplace(duplicate)
                                Conflict.Ignore -> dao.insertIgnore(duplicate)
                                Conflict.Abort -> dao.insertAbort(duplicate)
                            }
                            "${strategy.title}: повернуто rowId = $rowId"
                        } catch (e: Exception) {
                            "💥 ${strategy.title}: ${describe(e)}"
                        }
                    }
                },
            ),
            primaryFirst = true,
        )
        if (result.isNotEmpty()) Text(result)
        CompareTable(
            listOf("Стратегія", "Що станеться з id=1"),
            listOf(
                listOf("REPLACE", "Старий рядок замінено новим («Дубль»)"),
                listOf("IGNORE", "Лишився старий; insert повертає -1"),
                listOf("ABORT (за замовчуванням)", "SQLiteConstraintException — операція скасована"),
            ),
        )
    }
}

@Composable
private fun ProductRowsFromEntities(rows: List<ProductEntity>) {
    ProductRows(
        rows.map { Product(it.id, it.name, it.price, it.updatedAt) },
        emptyText = "(таблиця порожня)",
    )
}
