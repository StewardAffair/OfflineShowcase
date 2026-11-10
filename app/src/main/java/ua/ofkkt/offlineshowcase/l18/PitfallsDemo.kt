package ua.ofkkt.offlineshowcase.l18

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.NaiveProductEntity
import ua.ofkkt.offlineshowcase.data.ProductEntity
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 16: типові помилки при роботі з Room. */
@Composable
fun PitfallsDemo() {
    val db = rememberScratchDb()
    val dao = db.productDao()
    val naiveDao = db.naiveDao()
    val scope = rememberCoroutineScope()

    val naiveFlow = remember(naiveDao) { naiveDao.getAll() }
    val goodFlow = remember(dao) { dao.getAll() }
    val naive by naiveFlow.collectAsStateWithLifecycle(initialValue = emptyList<NaiveProductEntity>())
    val good by goodFlow.collectAsStateWithLifecycle(initialValue = emptyList<ProductEntity>())

    var mainThreadResult by remember { mutableStateOf("") }

    val remote = listOf(1 to "Яблука", 2 to "Молоко", 3 to "Хліб")

    DemoSection("Чотири найчастіші помилки") {
        CompareTable(
            listOf("Помилка", "Наслідок", "Рішення"),
            listOf(
                listOf("DAO з головного потоку", "IllegalStateException", "suspend або viewModelScope.launch"),
                listOf("Entity просочується в UI", "UI залежить від схеми БД", "Mapper Entity ↔ Domain"),
                listOf("Немає міграції", "Краш на пристрої зі старою базою", "Migration; destructive — лише в розробці"),
                listOf("Ігнорування OnConflictStrategy", "Дублікати або краш при refresh()", "REPLACE/IGNORE + правильний PrimaryKey"),
            ),
        )
    }

    DemoSection("1. Виклик DAO з головного потоку") {
        ActionRow(
            listOf(
                "Спробувати getAllBlocking() з UI" to {
                    mainThreadResult = try {
                        "Спрацювало: ${dao.getAllBlocking().size} рядків"
                    } catch (e: Exception) {
                        "💥 ${describe(e)}"
                    }
                    Unit
                },
            ),
        )
        if (mainThreadResult.isNotEmpty()) Text(mainThreadResult)
    }

    DemoSection("2. Entity просочується в UI") {
        CodeSnippet(
            """
            // ❌ Погано: Composable знає про Room
            @Composable fun List(items: List<ProductEntity>) { ... }

            // ✅ Добре: Composable працює з доменною моделлю
            @Composable fun List(items: List<Product>) { ... }
            // а Entity → Product перетворює Mapper у Repository
            """,
        )
    }

    DemoSection("3. Відсутність міграції") {
        Text("Відтворіть помилку на екрані «Міграції» (режим «Без міграції»).")
    }

    DemoSection(
        "4. Дублікати після refresh()",
        note = "Імітуємо три однакові refresh() з тим самим списком із сервера: [1 Яблука, 2 Молоко, 3 Хліб].",
    ) {
        ActionRow(
            listOf(
                "refresh() → таблиця з autoGenerate" to {
                    scope.launch {
                        naiveDao.insertAll(remote.map { NaiveProductEntity(remoteId = it.first, name = it.second) })
                    }
                },
                "refresh() → id із сервера як PrimaryKey" to {
                    scope.launch {
                        dao.insertAll(remote.map { ProductEntity(it.first, it.second, 0.0) })
                    }
                },
                "Очистити обидві" to { scope.launch { naiveDao.deleteAll(); dao.deleteAll() } },
            ),
            primaryFirst = true,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("❌ autoGenerate: ${naive.size} рядків")
                naive.forEach { Text("#${it.id} (сервер ${it.remoteId}) ${it.name}") }
            }
            Column(Modifier.weight(1f)) {
                Text("✅ PK із сервера: ${good.size} рядків")
                good.forEach { Text("#${it.id} ${it.name}") }
            }
        }
        Tip(
            "Натисніть кожну кнопку refresh() 2–3 рази. У таблиці з автогенерованим ключем REPLACE не спрацьовує " +
                "(id завжди новий) — список росте. Дублікати після повторного завантаження — типовий симптом " +
                "забутого onConflict або неправильного PrimaryKey.",
            TipKind.Warning,
        )
    }
}
