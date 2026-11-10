package ua.ofkkt.offlineshowcase.l18

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.RefreshResult
import ua.ofkkt.offlineshowcase.domain.Product
import ua.ofkkt.offlineshowcase.rememberContainer
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.LabeledSwitch
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind
import java.io.IOException

/** Слайди 2–3: чому локальне сховище обов'язкове + план лекції. */
@Composable
fun WhyLocalDemo() {
    val container = rememberContainer()
    val api = container.api
    val repo = container.repository
    val scope = rememberCoroutineScope()

    val online by api.online.collectAsStateWithLifecycle()
    val productsFlow = remember(repo) { repo.getProducts() }
    val cached by productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var naiveStatus by remember { mutableStateOf("Екран ще не відкривали") }
    var naiveItems by remember { mutableStateOf(emptyList<Product>()) }
    var syncStatus by remember { mutableStateOf("Синхронізації ще не було") }

    DemoSection(
        "Мережа ненадійна: метро, ліфт, роумінг",
        note = "Той самий екран зі списком товарів — у двох варіантах. Вимкніть мережу й порівняйте.",
    ) {
        LabeledSwitch("Мережа доступна", online) { api.setOnline(it) }
        Tip(
            "💡 Застосунок, що «вмирає» без інтернету, сприймається як низькоякісний — незалежно від того, " +
                "наскільки красивий його інтерфейс.",
        )
    }

    DemoSection(
        "Варіант А: екран читає прямо з мережі",
        note = "Кожне відкриття екрана = новий запит. Без мережі — порожній екран і помилка.",
    ) {
        ActionRow(
            listOf(
                "Відкрити екран (запит до мережі)" to {
                    scope.launch {
                        naiveStatus = "Завантаження з мережі…"
                        naiveItems = emptyList()
                        try {
                            naiveItems = api.getProducts()
                            naiveStatus = "Готово: ${naiveItems.size} товарів (і так щоразу)"
                        } catch (e: IOException) {
                            naiveStatus = "❌ ${e.message} — користувач бачить порожній екран"
                        }
                    }
                },
            ),
            primaryFirst = true,
        )
        Text(naiveStatus, style = MaterialTheme.typography.bodyMedium)
        ProductRows(naiveItems, emptyText = "(порожній екран)")
    }

    DemoSection(
        "Варіант Б: offline-first (Room + Repository)",
        note = "Екран завжди показує те, що лежить у Room. Мережа лише оновлює базу у фоні.",
    ) {
        ActionRow(
            listOf(
                "Синхронізувати у фоні (refresh)" to {
                    scope.launch {
                        syncStatus = "Синхронізація…"
                        syncStatus = when (val result = repo.refresh()) {
                            is RefreshResult.Success ->
                                "✅ Оновлено ${result.count} товарів"
                            is RefreshResult.Failed ->
                                "⚠️ ${result.message} — показуємо кеш"
                        }
                    }
                },
                "Очистити локальну базу" to { scope.launch { repo.resetLocal(); syncStatus = "Базу очищено" } },
            ),
            primaryFirst = true,
        )
        Text(syncStatus, style = MaterialTheme.typography.bodyMedium)
        ProductRows(cached, emptyText = "(у Room ще порожньо — один раз синхронізуйтесь із мережею)")
        Tip(
            "Спробуйте: 1) мережа ON → «Синхронізувати»; 2) мережа OFF; 3) знову «Відкрити екран» у варіанті А " +
                "(порожньо) — а варіант Б усе ще показує дані.",
            TipKind.Success,
        )
    }

    DemoSection("План лекції (слайд 3) і де це в застосунку") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("1. Room: Entity, DAO, Database → екрани «Entity», «DAO»")
            Text("2. Запити, Flow та корутини → екрани «Flow з DAO», «Корутини», «Міграції»")
            Text("3. Offline-First: single source of truth → екран «Offline-First»")
            Text("4. Стратегії синхронізації й конфліктів → екрани «Синхронізація», «Конфлікти», «Без мережі»")
            Text("5. Практика: схема Room для власного проєкту → екрани «Практика», «Тестування»")
        }
        Tip("🎯 Мета лекції — робочий offline-first шар даних для власного застосунку, готовий до Лаб5.")
    }
}
