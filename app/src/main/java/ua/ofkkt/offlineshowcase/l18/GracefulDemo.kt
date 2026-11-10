package ua.ofkkt.offlineshowcase.l18

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.rememberContainer
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.LabeledSwitch
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 13: Graceful Degradation — кешовані дані + банер «Офлайн» замість помилки. */
@Composable
fun GracefulDemo() {
    val container = rememberContainer()
    val repo = container.repository
    val api = container.api
    val scope = rememberCoroutineScope()

    val online by api.online.collectAsStateWithLifecycle()
    val offline by repo.isOffline.collectAsStateWithLifecycle()
    val lastError by repo.lastError.collectAsStateWithLifecycle()
    val productsFlow = remember(repo) { repo.getProducts() }
    val products by productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var autoRecover by rememberSaveable { mutableStateOf(true) }

    // Автоматичне відновлення: щойно мережа з'явилась, а Repository ще в офлайні — робимо refresh().
    LaunchedEffect(online, autoRecover, offline) {
        if (online && autoRecover && offline) repo.refresh()
    }

    DemoSection(
        "Екран продуктів без помилок",
        note = "Кроки: 1) мережа ON → refresh() (кеш наповнився); 2) мережа OFF → refresh(); 3) знову мережа ON.",
    ) {
        LabeledSwitch("Мережа доступна", online) { api.setOnline(it) }
        LabeledSwitch("Автоматично повторювати refresh() при появі мережі", autoRecover) { autoRecover = it }
        ActionRow(
            listOf(
                "refresh()" to { scope.launch { repo.refresh() } },
                "Очистити локальну базу" to { scope.launch { repo.resetLocal() } },
            ),
            primaryFirst = true,
        )

        if (offline) {
            Box(
                Modifier.fillMaxWidth().background(Color(0xFFFFE0B2), RoundedCornerShape(8.dp)).padding(10.dp),
            ) {
                Text("📴 Офлайн — показано збережені дані, вони можуть бути застарілими", color = Color(0xFF1B1B1B))
            }
        }
        ProductRows(products, emptyText = "(у Room порожньо: офлайн і кешу ще немає — це єдиний випадок «порожнього екрана»)")
    }

    DemoSection(
        "Логування без показу",
        note = "Мережеву помилку варто логувати для діагностики, але не завжди показувати як критичну.",
    ) {
        Text("Остання помилка Repository: ${lastError ?: "—"}")
        Tip(
            "«Показати старі дані з попередженням» — значно кращий UX, ніж «показати помилку і порожній екран». " +
                "Кешовані дані завжди кращі за нічого.",
            TipKind.Success,
        )
    }

    DemoSection("Repository та ViewModel (слайд 13)") {
        CodeSnippet(
            """
            // Repository: graceful degradation
            suspend fun refresh() {
                try {
                    val remote = api.getProducts()
                    productDao.insertAll(remote.map { it.toEntity() })
                    _isOffline.value = false
                } catch (e: IOException) {
                    // Мережева помилка — НЕ кидаємо виняток вгору
                    _isOffline.value = true
                    Log.w("Repository", "Network unavailable", e)
                }
            }

            // ViewModel: передаємо стан у UI
            val isOffline: StateFlow<Boolean> =
                repository.isOffline
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
            """,
        )
    }
}
