package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.CheckResult
import ua.ofkkt.offlineshowcase.data.DaoChecks
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 18: тестування Room на in-memory базі. */
@Composable
fun TestingDemo() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf(emptyList<CheckResult>()) }
    var running by remember { mutableStateOf(false) }

    DemoSection(
        "Запустіть DAO-тести прямо в застосунку",
        note = "Кожен тест створює СВОЮ in-memory базу (Room.inMemoryDatabaseBuilder) і закриває її в кінці.",
    ) {
        ActionRow(
            listOf(
                (if (running) "Виконується…" else "Запустити 5 тестів") to {
                    if (!running) {
                        scope.launch {
                            running = true
                            results = DaoChecks.runAll(context)
                            running = false
                        }
                    }
                    Unit
                },
            ),
            primaryFirst = true,
        )
        results.forEach { Text((if (it.passed) "✅ " else "❌ ") + it.name + " — " + it.message) }
        if (results.isNotEmpty()) {
            val passed = results.count { it.passed }
            Tip(
                "Пройдено $passed з ${results.size}",
                if (passed == results.size) TipKind.Success else TipKind.Error,
            )
        }
    }

    DemoSection("Тест зі слайда 18", note = "Справжній варіант для Android Studio: app/src/androidTest/.../DaoInMemoryTest.kt") {
        CodeSnippet(
            """
            @RunWith(AndroidJUnit4::class)
            class ProductDaoTest {
                private lateinit var db: AppDatabase
                private lateinit var dao: ProductDao

                @Before
                fun createDb() {
                    val context = ApplicationProvider.getApplicationContext<Context>()
                    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
                    dao = db.productDao()
                }

                @After
                fun closeDb() { db.close() }

                @Test
                fun insertAndGetAll() = runTest {
                    val entity = ProductEntity(id = 1, name = "Test", price = 9.99)
                    dao.insertAll(listOf(entity))
                    val result = dao.getAll().first()
                    assertEquals(1, result.size)
                    assertEquals("Test", result[0].name)
                }
            }
            """,
        )
    }

    DemoSection("Переваги in-memory тестів") {
        CompareTable(
            listOf("Перевага", "Пояснення"),
            listOf(
                listOf("Реальний SQL без моків", "Перевіряється справжній @Query; помилка в запиті — тест падає"),
                listOf("Ізольованість", "Кожен тест починається з чистої бази"),
                listOf("Швидкість", "Набагато швидше за файлову базу"),
                listOf("Перевірка міграцій", "Окремий інструмент MigrationTestHelper"),
            ),
        )
    }
}
