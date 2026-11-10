package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ua.ofkkt.offlineshowcase.domain.generateRoomSkeleton
import ua.ofkkt.offlineshowcase.domain.parseFields
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 17: практика — схема Room для власного проєкту. */
@Composable
fun PracticeDemo() {
    var entityName by rememberSaveable { mutableStateOf("Book") }
    var fieldsSpec by rememberSaveable { mutableStateOf("title:String, author:String, pages:Int, price:Double") }

    val fields = remember(fieldsSpec) { parseFields(fieldsSpec) }
    val code = remember(entityName, fields) { generateRoomSkeleton(entityName, fields) }

    DemoSection("План практичного заняття", note = "Позначайте виконані кроки.") {
        CheckList(
            listOf(
                "1. Проєктування Entity: поля, первинний ключ, чи потрібен cachedAt",
                "2. Написання DAO: getAll(): Flow<List<…>>, insertAll(), deleteAll()",
                "3. Реалізація Repository: getItems() — Flow з Room, refresh() — мережа → Room + обробка помилки",
                "4. Перевірка через Flow: ViewModel → StateFlow → UI оновлюється після refresh()",
            ),
        )
        Tip("Результат цього заняття — прямий фундамент для Лаб5: мережевий шар + Room + перевірка offline-поведінки.")
    }

    DemoSection(
        "Генератор каркаса для вашої сутності",
        note = "Введіть назву сутності й поля (імʼя:Тип через кому) — отримаєте Entity, DAO і Repository.",
    ) {
        OutlinedTextField(
            value = entityName,
            onValueChange = { entityName = it },
            label = { Text("Сутність (напр. Book, Task, Note)") },
            singleLine = true,
        )
        OutlinedTextField(
            value = fieldsSpec,
            onValueChange = { fieldsSpec = it },
            label = { Text("Поля: name:Type, ...") },
        )
        Text("Розпізнано полів: ${fields.size} (id і cachedAt додаються автоматично)")
        CodeSnippet(code)
        Tip(
            "Це лише відправна точка. Подумайте самі: що буде первинним ключем? Чи потрібен updatedAt для " +
                "стратегії конфліктів? Які поля шукатимуть — їм потрібен @Index.",
            TipKind.Info,
        )
    }
}
