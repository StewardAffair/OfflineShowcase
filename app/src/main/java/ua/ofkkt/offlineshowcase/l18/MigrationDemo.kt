package ua.ofkkt.offlineshowcase.l18

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.MigrationMode
import ua.ofkkt.offlineshowcase.data.MigrationReport
import ua.ofkkt.offlineshowcase.rememberContainer
import ua.ofkkt.offlineshowcase.ui.ChoiceChips
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.PrimaryButton
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайд 9: міграції. Експеримент справжній: файл БД версії 1 відкривається як версія 2. */
@Composable
fun MigrationDemo() {
    val lab = rememberContainer().migrationLab
    val scope = rememberCoroutineScope()

    var modeName by rememberSaveable { mutableStateOf(MigrationMode.WithMigration.name) }
    val mode = MigrationMode.valueOf(modeName)
    var report by remember { mutableStateOf<MigrationReport?>(null) }
    var running by remember { mutableStateOf(false) }

    DemoSection(
        "Експеримент: оновлення схеми v1 → v2",
        note = "Крок 1: створюємо БД v1 і кладемо 3 товари. Крок 2: відкриваємо той самий файл як v2 " +
            "(додано стовпець price) — обраним способом.",
    ) {
        ChoiceChips(MigrationMode.entries, mode, { modeName = it.name }, { it.label })
        PrimaryButton(
            text = if (running) "Виконується…" else "Запустити експеримент",
            enabled = !running,
            onClick = {
                scope.launch {
                    running = true
                    report = lab.run(mode)
                    running = false
                }
            },
        )
        val result = report
        if (result != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Було у v1:")
                CodeSnippet(result.before.joinToString("\n"))
                if (result.error != null) {
                    Tip("Застосунок «впав»: ${result.error}", TipKind.Error)
                } else {
                    Text("Стало у v2:")
                    val after = result.after.orEmpty()
                    CodeSnippet(if (after.isEmpty()) "(порожньо — дані втрачено)" else after.joinToString("\n"))
                    Tip(
                        if (after.isEmpty()) "База пересоздана: усі дані користувача втрачено."
                        else "Дані збережено; новий стовпець price отримав значення за замовчуванням 0.0.",
                        if (after.isEmpty()) TipKind.Warning else TipKind.Success,
                    )
                }
            }
        }
    }

    DemoSection("Три сценарії") {
        CompareTable(
            listOf("Варіант", "Що відбувається"),
            listOf(
                listOf("З міграцією", "Room виконує SQL до існуючої бази; дані зберігаються. Обов'язково для production."),
                listOf("Без міграції", "Room не знає, як оновити схему → IllegalStateException."),
                listOf("fallbackToDestructiveMigration", "Room видаляє і створює базу заново. Лише для розробки."),
            ),
        )
        Tip(
            "У production-застосунку fallbackToDestructiveMigration() неприйнятний — користувач втратить усі дані.",
            TipKind.Error,
        )
    }

    DemoSection("Код із слайда 9 (файл data/MigrationLab.kt)") {
        CodeSnippet(
            """
            // Версія 1 → 2: додано стовпець price
            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE products " +
                        "ADD COLUMN price REAL NOT NULL DEFAULT 0.0"
                    )
                }
            }

            // Реєстрація при побудові бази
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                .addMigrations(MIGRATION_1_2)
                .build()

            // Лише для розробки!
            // .fallbackToDestructiveMigration()
            """,
        )
    }
}
