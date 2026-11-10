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
import ua.ofkkt.offlineshowcase.data.RefreshResult
import ua.ofkkt.offlineshowcase.domain.ConflictStrategy
import ua.ofkkt.offlineshowcase.rememberContainer
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.ChoiceChips
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 12: обробка конфліктів — Last-Write-Wins, Timestamp-based, Merge/CRDT. */
@Composable
fun ConflictDemo() {
    val container = rememberContainer()
    val repo = container.repository
    val api = container.api
    val scope = rememberCoroutineScope()

    val online by api.online.collectAsStateWithLifecycle()
    val server by api.server.collectAsStateWithLifecycle()
    val productsFlow = remember(repo) { repo.getProducts() }
    val local by productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var strategyName by rememberSaveable { mutableStateOf(ConflictStrategy.ServerWins.name) }
    val strategy = ConflictStrategy.valueOf(strategyName)
    var notes by remember { mutableStateOf(emptyList<String>()) }

    val localItem = local.firstOrNull { it.id == 1 }
    val serverItem = server.firstOrNull { it.id == 1 }

    DemoSection(
        "Три підходи до конфліктів",
        note = "Конфлікт: ті самі дані змінено і на сервері, і локально (офлайн).",
    ) {
        CompareTable(
            listOf("Стратегія", "Суть", "Мінус"),
            listOf(
                listOf("Last-Write-Wins", "Сервер повністю замінює локальні дані", "Локальні зміни можуть загубитись"),
                listOf("Timestamp-based", "Виграє версія з пізнішим updatedAt", "Потрібна синхронізація часу"),
                listOf("Merge / CRDT", "Об'єднання змін за правилами", "Найскладніше; за межами курсу"),
            ),
        )
        Tip("Для навчального проєкту зазвичай достатньо простого перезапису локальних даних серверними.")
    }

    DemoSection(
        "Сценарій на справжніх Room + «сервері»",
        note = "Працюємо з товаром #1. Кроки можна виконувати в різному порядку — і від порядку залежить результат.",
    ) {
        ChoiceChips(ConflictStrategy.entries, strategy, { strategyName = it.name }, { it.label })

        ActionRow(
            listOf(
                "0. Скинути й завантажити" to {
                    scope.launch {
                        api.reset()
                        repo.resetLocal()
                        repo.refresh()
                        notes = emptyList()
                    }
                },
                "A. Вимкнути мережу" to { api.setOnline(false) },
                "B. Локально: ціна #1 = 99" to { scope.launch { repo.updatePriceLocally(1, 99.0) } },
                "C. Сервер (інший пристрій): ціна #1 = 77" to { api.serverEditPrice(1, 77.0) },
                "D. Увімкнути мережу й refresh" to {
                    scope.launch {
                        api.setOnline(true)
                        notes = when (val result = repo.refresh(strategy = strategy)) {
                            is RefreshResult.Success ->
                                result.notes.ifEmpty { listOf("Конфліктів не було") }
                            is RefreshResult.Failed -> listOf(result.message)
                        }
                    }
                },
            ),
            primaryFirst = true,
        )
        Text("Мережа: ${if (online) "є" else "немає"}")

        CompareTable(
            listOf("Де", "Ціна #1", "Змінено о"),
            listOf(
                listOf("Локально (Room)", localItem?.let { uah(it.price) } ?: "—", clockTime(localItem?.updatedAt ?: 0L)),
                listOf("Сервер", serverItem?.let { uah(it.price) } ?: "—", clockTime(serverItem?.updatedAt ?: 0L)),
            ),
        )
        notes.forEach { Text("• $it") }

        Tip(
            "Спробуйте: 0 → A → B → C → D. Last-Write-Wins: ціна стане 77 (локальна 99 втрачена). " +
                "Timestamp-based: C було пізніше за B → теж виграє сервер. Тепер 0 → A → C → B → D: " +
                "локальна зміна пізніша → при Timestamp-based виграє 99 і відправляється на сервер.",
            TipKind.Info,
        )
    }

    DemoSection("Де це в коді") {
        Text(
            "domain/Sync.kt → resolveConflict(local, remote, strategy) — чиста функція, покрита unit-тестами.\n" +
                "data/ProductRepository.kt → refresh(strategy) викликає її для кожного товару.",
        )
        Tip(
            "Firebase Realtime Database і Supabase Realtime частково знімають цю проблему власними механізмами " +
                "синхронізації й підписками на зміни в реальному часі.",
        )
    }
}
