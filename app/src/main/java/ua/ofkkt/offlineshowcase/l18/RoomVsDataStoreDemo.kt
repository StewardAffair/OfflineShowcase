package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.OutlinedTextField
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
import ua.ofkkt.offlineshowcase.rememberContainer
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.LabeledSwitch
import ua.ofkkt.offlineshowcase.ui.SecondaryButton
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

private data class StorageItem(val text: String, val isRoom: Boolean, val why: String)

private val storageItems = listOf(
    StorageItem("Список продуктів", true, "табличні, реляційні дані"),
    StorageItem("Тема інтерфейсу (світла/темна)", false, "просте налаштування, ключ-значення"),
    StorageItem("Повідомлення чату", true, "багато записів, потрібні запити й фільтрація"),
    StorageItem("Мова застосунку", false, "одне значення за ключем"),
    StorageItem("Користувачі та їхні замовлення (JOIN)", true, "зв'язані таблиці й JOIN"),
    StorageItem("Час останньої синхронізації", false, "одне число; саме так зберігається в цьому проєкті"),
    StorageItem("Історія покупок із фільтром за датою", true, "складний SELECT із фільтрацією"),
    StorageItem("Токен авторизації", false, "ключ-значення (у production — у зашифрованому вигляді)"),
)

/** Слайд 14: Room vs DataStore. */
@Composable
fun RoomVsDataStoreDemo() {
    val container = rememberContainer()
    val settings = container.settings
    val repo = container.repository
    val scope = rememberCoroutineScope()

    val compactFlow = remember(settings) { settings.compactList }
    val nameFlow = remember(settings) { settings.userName }
    val syncFlow = remember(settings) { settings.lastSync }
    val compact by compactFlow.collectAsStateWithLifecycle(initialValue = false)
    val userName by nameFlow.collectAsStateWithLifecycle(initialValue = "")
    val lastSync by syncFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val productsFlow = remember(repo) { repo.getProducts() }
    val products by productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var draft by rememberSaveable { mutableStateOf("") }
    var index by rememberSaveable { mutableStateOf(0) }
    var score by rememberSaveable { mutableStateOf(0) }
    var feedback by rememberSaveable { mutableStateOf("") }

    DemoSection("Порівняння") {
        CompareTable(
            listOf("Критерій", "Room", "DataStore"),
            listOf(
                listOf("Тип даних", "Структуровані, реляційні", "Прості налаштування"),
                listOf("API", "SQL через DAO", "Flow + типізовані ключі"),
                listOf("Запити", "SELECT, JOIN, фільтрація", "Лише читання/запис за ключем"),
                listOf("Міграції", "Потрібні при зміні схеми", "Не потрібні (немає схеми)"),
                listOf("Типові дані", "Продукти, повідомлення", "Тема, токен, мова"),
                listOf("Замінює", "SQLiteOpenHelper", "SharedPreferences"),
            ),
        )
    }

    DemoSection(
        "Обидва сховища в цьому проєкті",
        note = "Таблиця products — у Room; налаштування й час синхронізації — у DataStore (файл settings.preferences_pb).",
    ) {
        Text("Room: ${products.size} товарів у таблиці products")
        Text("DataStore: lastSync = ${clockTime(lastSync)}")
        LabeledSwitch("Компактний список (DataStore, Boolean)", compact) { scope.launch { settings.setCompactList(it) } }
        Text("Ім'я користувача (DataStore, String): $userName")
        OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Нове ім'я") }, singleLine = true)
        ActionRow(listOf("Зберегти в DataStore" to { scope.launch { settings.setUserName(draft.ifBlank { "Гість" }) } }))
        Tip("Закрийте й заново відкрийте застосунок: перемикач і ім'я збережуться — DataStore живе на диску.", TipKind.Info)
    }

    DemoSection("Тренажер: Room чи DataStore?", note = "Рахунок: $score з ${storageItems.size}") {
        if (index >= storageItems.size) {
            Text("Готово! Результат: $score з ${storageItems.size}")
            SecondaryButton("Спробувати ще раз", { index = 0; score = 0; feedback = "" })
        } else {
            val item = storageItems[index]
            Text("Де зберігати: «${item.text}»?")
            ActionRow(
                listOf(
                    "Room" to { answer(item, true, { feedback = it }, { score += 1 }); index += 1; Unit },
                    "DataStore" to { answer(item, false, { feedback = it }, { score += 1 }); index += 1; Unit },
                ),
                primaryFirst = true,
            )
        }
        if (feedback.isNotEmpty()) Text(feedback)
    }
}

private fun answer(item: StorageItem, choseRoom: Boolean, setFeedback: (String) -> Unit, addScore: () -> Unit) {
    val correct = item.isRoom == choseRoom
    if (correct) addScore()
    val right = if (item.isRoom) "Room" else "DataStore"
    setFeedback((if (correct) "✅ Вірно: " else "❌ Ні, це $right: ") + item.why)
}
