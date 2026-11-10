package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.SecondaryButton
import ua.ofkkt.offlineshowcase.ui.Tip

@Composable
private fun Question(title: String, answer: String) {
    var open by remember { mutableStateOf(false) }
    Text(title)
    SecondaryButton(if (open) "Сховати відповідь" else "Показати відповідь", { open = !open })
    if (open) Tip(answer)
}

/** Слайди 19–20: підсумок, контрольні питання, домашнє завдання. */
@Composable
fun SummaryDemo() {
    DemoSection("Підсумок: ключові тези (слайд 19)") {
        CompareTable(
            listOf("Теза", "Де побачити в застосунку"),
            listOf(
                listOf("Room = Entity + DAO + Database", "Екрани «Entity», «DAO»"),
                listOf("Flow з DAO — реактивний UI", "Екран «Flow з DAO»"),
                listOf("Offline-First: UI читає тільки з Room", "Екрани «Offline-First», «Чому локальне сховище»"),
                listOf("Graceful degradation", "Екран «Без мережі»"),
            ),
        )
        Tip("Далі — Лекція 19: Flow та StateFlow — як дані течуть від Repository через ViewModel до UI.")
    }

    DemoSection("Контрольні питання (слайд 20)", note = "Спершу відповідайте самі, потім перевіряйте.") {
        Question(
            "1. Чому UI не повинен читати дані напряму з мережі в offline-first підході? Що є єдиним джерелом правди і чому саме Room?",
            "Мережа ненадійна й повільна: екран без з'єднання був би порожнім. Єдине джерело правди — локальна база: " +
                "дані є завжди, а мережа лише оновлює її у фоні. Room дає типобезпечний доступ і реактивні Flow.",
        )
        Question(
            "2. Що станеться, якщо викликати Room-запит з головного потоку без suspend або без Flow? Яке виключення й чому?",
            "IllegalStateException: Cannot access database on the main thread… Room забороняє це, щоб запит " +
                "не підвішував інтерфейс. Рішення: suspend-функції або Flow, виклик із viewModelScope.launch. " +
                "(Перевірте на екрані «Корутини».)",
        )
        Question(
            "3. Яка різниця між Room і DataStore? Наведіть приклади даних для кожного.",
            "Room — реляційні таблиці й складні запити (продукти, повідомлення, користувачі), потрібні міграції. " +
                "DataStore — прості пари ключ-значення (тема, мова, токен, час синхронізації), схеми немає.",
        )
    }

    DemoSection("Домашнє завдання (слайд 20)") {
        CheckList(
            listOf(
                "01. Room Entity та DAO для основних сутностей власного проєкту (є Flow-метод для читання списку)",
                "02. Письмово: яку стратегію конфліктів обрано (last-write-wins чи timestamp-based) і чому",
                "03. Хоча б один in-memory Room-тест DAO: insert і getAll() повертають правильні дані",
            ),
        )
    }

    DemoSection("Готовність до Лаб5") {
        CheckList(
            listOf(
                "Room Entity",
                "DAO",
                "AppDatabase",
                "Repository з offline-first стратегією для основних сутностей",
            ),
        )
    }
}
