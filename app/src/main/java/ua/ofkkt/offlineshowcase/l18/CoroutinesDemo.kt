package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.ofkkt.offlineshowcase.data.ProductEntity
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.EventLog
import ua.ofkkt.offlineshowcase.ui.LogView
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 8: Room і корутини; заборона запитів з UI-потоку. */
@Composable
fun CoroutinesDemo() {
    val db = rememberScratchDb()
    val dao = db.productDao()
    val scope = rememberCoroutineScope()
    val log = remember { EventLog() }

    var ticks by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    // «Годинник UI-потоку»: якщо головний потік заблокувати, лічильник зупиниться.
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            ticks += 1
        }
    }

    LaunchedEffect(dao) { dao.insertAll(sampleEntities()) }

    DemoSection(
        "Правило: DAO — не з UI-потоку",
        note = "Room за замовчуванням забороняє запити з main thread, щоб інтерфейс не зависав.",
    ) {
        Text("UI-тіки (кожні 50 мс): $ticks")
        ActionRow(
            listOf(
                "❌ getAllBlocking() прямо з UI" to {
                    // Виклик у обробнику кліку = головний потік.
                    try {
                        val rows = dao.getAllBlocking()
                        log.add("Несподівано спрацювало: ${rows.size} рядків")
                    } catch (e: Exception) {
                        log.add("💥 ${describe(e)}")
                    }
                },
                "✅ suspend getById() у корутині" to {
                    scope.launch {
                        val row = dao.getById(1)
                        log.add("suspend-запит виконано: ${row?.name}")
                    }
                },
                "✅ blocking у withContext(IO)" to {
                    scope.launch {
                        val rows = withContext(Dispatchers.IO) { dao.getAllBlocking() }
                        log.add("blocking-запит у фоні: ${rows.size} рядків")
                    }
                },
            ),
        )
        LogView(log, "Результати")
        Tip(
            "Не-suspend метод DAO з головного потоку кидає IllegalStateException: " +
                "«Cannot access database on the main thread…». Спробуйте першу кнопку.",
            TipKind.Warning,
        )
    }

    DemoSection(
        "suspend-запис не блокує інтерфейс",
        note = "Вставляємо 20 000 рядків. Room сам переносить роботу на фоновий потік — тіки далі йдуть.",
    ) {
        ActionRow(
            listOf(
                "Вставити 20 000 рядків (suspend)" to {
                    if (!busy) {
                        scope.launch {
                            busy = true
                            val startedAt = System.currentTimeMillis()
                            val ticksBefore = ticks
                            val big = List(20_000) { i -> ProductEntity(1000 + i, "Товар $i", i.toDouble()) }
                            dao.insertAll(big)
                            val elapsed = System.currentTimeMillis() - startedAt
                            log.add("Вставлено 20 000 рядків за $elapsed мс; за цей час UI зробив ${ticks - ticksBefore} тіків")
                            busy = false
                        }
                    }
                    Unit
                },
            ),
            primaryFirst = true,
        )
        if (busy) Text("Йде запис у базу…")
    }

    DemoSection("Як це виглядає в коді (слайд 8)") {
        CodeSnippet(
            """
            // Repository
            suspend fun refresh() {
                val remote = api.getProducts()
                val entities = remote.map { it.toEntity() }
                productDao.deleteAll()
                productDao.insertAll(entities)
            }

            // ViewModel
            fun refreshProducts() {
                viewModelScope.launch {
                    repository.refresh()   // suspend
                }
            }

            // Неправильно: виклик з UI-потоку
            // productDao.getAllBlocking()  // IllegalStateException
            """,
        )
        Tip("Write-операції — suspend fun; read-операції — Flow. RoomDatabase будується один раз (singleton через DI).")
    }
}
