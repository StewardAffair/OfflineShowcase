package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.ProductEntity
import ua.ofkkt.offlineshowcase.domain.Product
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.EventLog
import ua.ofkkt.offlineshowcase.ui.LogView
import ua.ofkkt.offlineshowcase.ui.Tip

/** Слайд 7: Flow, повернений з DAO — UI підписується один раз. */
@Composable
fun FlowDemo() {
    val db = rememberScratchDb()
    val dao = db.productDao()
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf(emptyList<ProductEntity>()) }
    var emissions by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf(10) }
    val log = remember { EventLog() }

    // Підписка ОДИН раз. Далі ми лише пишемо в базу — і жодного ручного «оновити список».
    LaunchedEffect(dao) {
        dao.getAll().collect { list ->
            items = list
            emissions += 1
            log.add("emit #$emissions → ${list.size} шт.: ${list.joinToString { it.name }}")
        }
    }

    DemoSection("Ланцюжок", note = "INSERT → Room помічає зміну → новий emit у Flow → ViewModel → UI перемальовується.") {
        Text("Кількість emit-ів з моменту відкриття екрана: $emissions")
        ProductRows(
            items.map { Product(it.id, it.name, it.price, it.updatedAt) },
            emptyText = "(порожньо)",
        )
        ActionRow(
            listOf(
                "INSERT новий товар" to {
                    scope.launch {
                        dao.insertAll(listOf(ProductEntity(nextId, "Товар $nextId", nextId * 3.5)))
                        nextId += 1
                    }
                },
                "UPDATE: перший товар +1 ₴" to {
                    scope.launch { items.firstOrNull()?.let { dao.update(it.copy(price = it.price + 1)) } }
                },
                "DELETE: перший товар" to {
                    scope.launch { items.firstOrNull()?.let { dao.delete(it) } }
                },
                "DELETE ALL" to { scope.launch { dao.deleteAll() } },
            ),
            primaryFirst = true,
        )
        LogView(log, "Що отримав колектор Flow")
        Tip("Жодна кнопка не оновлює список вручну: екран реагує лише на нові значення з Flow.")
    }

    DemoSection("Як це написано (слайд 7)") {
        CodeSnippet(
            """
            // DAO
            @Query("SELECT * FROM products ORDER BY product_name ASC")
            fun getAll(): Flow<List<ProductEntity>>

            // Repository
            fun getProducts(): Flow<List<Product>> =
                productDao.getAll().map { entities -> entities.map { it.toDomain() } }

            // ViewModel
            val products: StateFlow<List<Product>> =
                repository.getProducts().stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

            // Compose
            val products by viewModel.products.collectAsStateWithLifecycle()
            """,
        )
    }
}
