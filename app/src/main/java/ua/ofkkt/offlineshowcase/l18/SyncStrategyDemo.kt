package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.RefreshResult
import ua.ofkkt.offlineshowcase.domain.CACHE_TIMEOUT_MS
import ua.ofkkt.offlineshowcase.rememberContainer
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.EventLog
import ua.ofkkt.offlineshowcase.ui.LabeledSwitch
import ua.ofkkt.offlineshowcase.ui.LogView
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

private fun describeResult(result: RefreshResult): String = when (result) {
    is RefreshResult.Success -> "✅ refresh(): оновлено ${result.count} товарів"
    is RefreshResult.Failed -> "⚠️ refresh(): ${result.message}"
}

/** Слайд 11: стратегії синхронізації — ручна, автоматична при вході, фонова. */
@Composable
fun SyncStrategyDemo() {
    val container = rememberContainer()
    val repo = container.repository
    val api = container.api
    val clock = container.clock
    val scope = rememberCoroutineScope()

    val online by api.online.collectAsStateWithLifecycle()
    val requests by api.requests.collectAsStateWithLifecycle()
    val lastSyncFlow = remember(repo) { repo.lastSync }
    val lastSync by lastSyncFlow.collectAsStateWithLifecycle(initialValue = 0L)

    var now by remember { mutableStateOf(clock.now()) }
    var background by rememberSaveable { mutableStateOf(false) }
    val log = remember { EventLog() }

    LaunchedEffect(Unit) {
        while (true) {
            now = clock.now()
            delay(1000)
        }
    }

    val backgroundEnabled by rememberUpdatedState(background)
    LaunchedEffect(background) {
        while (backgroundEnabled) {
            delay(10_000)
            if (backgroundEnabled) log.add("⏰ фон: " + describeResult(repo.refresh()))
        }
    }

    val ageMinutes = if (lastSync == 0L) null else (now - lastSync) / 60_000
    val timeoutMinutes = CACHE_TIMEOUT_MS / 60_000

    DemoSection("Стан кешу", note = "Година в цьому демо «перемотується», щоб не чекати 30 хвилин.") {
        LabeledSwitch("Мережа доступна", online) { api.setOnline(it) }
        Text("Остання синхронізація: ${clockTime(lastSync)}")
        Text("Вік кешу: ${ageMinutes?.let { "$it хв" } ?: "кешу ще немає"} · поріг: $timeoutMinutes хв · запитів до сервера: $requests")
    }

    DemoSection("1. Ручна (pull-to-refresh)", note = "Користувач явно просить оновлення. Найпростіший підхід.") {
        ActionRow(
            listOf("Потягнути для оновлення" to { scope.launch { log.add("👆 " + describeResult(repo.refresh())) } }),
            primaryFirst = true,
        )
    }

    DemoSection(
        "2. Автоматична при вході",
        note = "Repository перевіряє час останньої синхронізації при кожному відкритті екрана; мережа — лише якщо кеш застарів.",
    ) {
        ActionRow(
            listOf(
                "Відкрити екран (авто-перевірка)" to {
                    scope.launch {
                        if (repo.shouldRefresh()) {
                            log.add("🚪 кеш застарів → refresh")
                            log.add("🚪 " + describeResult(repo.refresh()))
                        } else {
                            log.add("🚪 кеш свіжий → мережу не чіпаємо")
                        }
                    }
                },
                "Перемотати час +10 хв" to { clock.advance(10 * 60_000L); now = clock.now(); Unit },
                "Перемотати час +31 хв" to { clock.advance(31 * 60_000L); now = clock.now(); Unit },
            ),
            primaryFirst = true,
        )
        Tip("Спробуйте: «Потягнути» → «Відкрити» (свіжий) → +31 хв → «Відкрити» (застарів → запит).", TipKind.Info)
    }

    DemoSection(
        "3. Фонова (WorkManager)",
        note = "Система планує синхронізацію навіть коли застосунок закрито. Тут — лише імітація таймером, поки відкритий екран.",
    ) {
        LabeledSwitch("Імітувати фонову синхронізацію кожні 10 с", background) { background = it }
        CodeSnippet(
            """
            // Реальний варіант (виходить за межі базового курсу)
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, request)
            """,
        )
    }

    LogView(log, "Журнал синхронізацій")

    DemoSection("Типи синхронізації (порівняння)") {
        CompareTable(
            listOf("Тип", "Коли", "Складність"),
            listOf(
                listOf("Ручна", "Користувач тягне список", "Мінімальна"),
                listOf("Автоматична при вході", "Кеш старіший за поріг", "Низька"),
                listOf("Фонова (WorkManager)", "За розкладом, навіть закритий застосунок", "Вища; production"),
            ),
        )
        CodeSnippet(
            """
            suspend fun refresh() {
                try {
                    val remoteProducts = api.getProducts()
                    productDao.deleteAll()
                    productDao.insertAll(remoteProducts.map { it.toEntity() })
                    prefs.saveLastSyncTime(System.currentTimeMillis())
                } catch (e: IOException) {
                    Log.w("Sync", "Refresh failed", e)
                }
            }

            fun shouldRefresh(): Boolean {
                val lastSync = prefs.getLastSyncTime()
                val elapsed = System.currentTimeMillis() - lastSync
                return elapsed > CACHE_TIMEOUT_MS // 30 хв
            }
            """,
        )
    }
}
