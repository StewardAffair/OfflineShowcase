package ua.ofkkt.offlineshowcase.l18

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.RefreshResult
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

/** Слайди 10 і 15: single source of truth та архітектурна діаграма offline-first. */
@Composable
fun SsotDemo() {
    val container = rememberContainer()
    val repo = container.repository
    val api = container.api
    val scope = rememberCoroutineScope()

    val online by api.online.collectAsStateWithLifecycle()
    val requests by api.requests.collectAsStateWithLifecycle()
    val server by api.server.collectAsStateWithLifecycle()
    val offlineFlag by repo.isOffline.collectAsStateWithLifecycle()
    val lastSyncFlow = remember(repo) { repo.lastSync }
    val lastSync by lastSyncFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val productsFlow = remember(repo) { repo.getProducts() }
    val local by productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var atomic by rememberSaveable { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    val log = remember { EventLog() }

    LaunchedEffect(repo) {
        repo.getProducts().collect { list -> log.add("Flow emit: у Room ${list.size} товарів") }
    }

    DemoSection("Архітектура offline-first (слайд 15)") {
        CodeSnippet(
            """
            Мережевий шар        лише ЗАПИС у Room
            (Retrofit/Ktor/...)        │
                  ▲                    ▼
                  └──── Repository ─► Room ──Flow──► ViewModel ──► UI
                          refresh()   (єдине          StateFlow    Compose
                                      джерело правди)

            UI НІКОЛИ не звертається до мережі напряму.
            """,
        )
    }

    DemoSection("4 кроки single source of truth (слайд 10)") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. Мережевий виклик — API повертає дані; Repository отримує відповідь.")
            Text("2. Room оновлюється — Repository виконує insertAll()/upsert().")
            Text("3. Flow емітить нові дані — активні колектори отримують список.")
            Text("4. UI перемальовується — Compose реагує на новий стан.")
        }
    }

    DemoSection(
        "Живий експеримент",
        note = "Нижче два списки: те, що бачить UI (Room), і те, що лежить на «сервері». Вони розходяться, доки ви не зробите refresh().",
    ) {
        LabeledSwitch("Мережа доступна", online) { api.setOnline(it) }
        LabeledSwitch("Атомарна заміна (у транзакції)", atomic) { atomic = it }
        Text("Запитів до сервера: $requests · остання синхронізація: ${clockTime(lastSync)}")
        Text(if (offlineFlag) "Repository: офлайн-режим (остання спроба не вдалась)" else "Repository: онлайн")

        ActionRow(
            listOf(
                (if (refreshing) "Синхронізація…" else "refresh()") to {
                    if (!refreshing) {
                        scope.launch {
                            refreshing = true
                            val result = repo.refresh(atomic = atomic)
                            log.add(
                                when (result) {
                                    is RefreshResult.Success -> "refresh(): ✅ ${result.count} товарів"
                                    is RefreshResult.Failed -> "refresh(): ⚠️ ${result.message}"
                                },
                            )
                            refreshing = false
                        }
                    }
                    Unit
                },
                "Інший пристрій: додати товар" to { api.serverAdd("Новинка ${server.size + 1}", 100.0 + server.size) },
                "Інший пристрій: ціна #1 +5" to {
                    val first = server.firstOrNull { it.id == 1 }
                    if (first != null) api.serverEditPrice(1, first.price + 5)
                    Unit
                },
                "Скинути демо" to {
                    scope.launch {
                        api.reset()
                        repo.resetLocal()
                        log.clear()
                    }
                },
            ),
            primaryFirst = true,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Room (бачить UI): ${local.size}")
                ProductRows(local, emptyText = "(порожньо)")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Сервер: ${server.size}")
                ProductRows(server)
            }
        }
        LogView(log, "Emit-и Flow із Room")
        Tip(
            "Спробуйте: refresh() онлайн → Room збігається із сервером. «Інший пристрій: додати» — сервер виріс, " +
                "а UI ні, поки не буде refresh(). Вимкніть мережу — UI все одно показує Room.",
            TipKind.Info,
        )
        Tip(
            "Вимкніть «Атомарна заміна»: deleteAll() і insertAll() стануть двома окремими записами, і в журналі " +
                "може з'явитись emit з 0 товарів — користувач побачить «моргання» порожнього списку. " +
                "Транзакція (db.withTransaction) дає один emit. Пауза між кроками в демо навмисно збільшена.",
            TipKind.Warning,
        )
    }

    DemoSection("Ролі шарів (слайд 15)") {
        CompareTable(
            listOf("Шар", "Роль"),
            listOf(
                listOf("Мережевий шар", "Retrofit, Ktor, Firebase або Supabase — лише для запису в Room"),
                listOf("Repository", "Оркеструє запити: мережа → Room → Flow. Єдина точка входу для ViewModel"),
                listOf("Room Database", "Єдине джерело правди. UI читає лише з нього"),
                listOf("ViewModel → UI", "Перетворює Flow на StateFlow; UI підписується й оновлюється автоматично"),
            ),
        )
    }
}
