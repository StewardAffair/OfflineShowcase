package ua.ofkkt.offlineshowcase.l18

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import ua.ofkkt.offlineshowcase.data.ProductEntity
import ua.ofkkt.offlineshowcase.ui.ActionRow
import ua.ofkkt.offlineshowcase.ui.CodeSnippet
import ua.ofkkt.offlineshowcase.ui.CompareTable
import ua.ofkkt.offlineshowcase.ui.DemoSection
import ua.ofkkt.offlineshowcase.ui.Tip
import ua.ofkkt.offlineshowcase.ui.TipKind

/** Слайди 4–5: Room = Entity + DAO + Database; Entity описує таблицю. */
@Composable
fun EntityDemo() {
    val db = rememberScratchDb()
    val dao = db.productDao()
    val scope = rememberCoroutineScope()

    var tableSql by remember { mutableStateOf(emptyList<List<String>>()) }
    var columns by remember { mutableStateOf(emptyList<List<String>>()) }
    var rawRows by remember { mutableStateOf(emptyList<List<String>>()) }
    var ignoreResult by remember { mutableStateOf("") }

    LaunchedEffect(db) {
        tableSql = db.rawQuery("SELECT name, sql FROM sqlite_master WHERE tbl_name = 'products'")
        columns = db.rawQuery("PRAGMA table_info(products)")
    }

    DemoSection(
        "Три компоненти Room (слайд 4)",
        note = "Усі три живуть у проєкті: ProductEntity (таблиця), ProductDao (запити), AppDatabase (об'єднує їх).",
    ) {
        CompareTable(
            listOf("Компонент", "Анотація", "Роль"),
            listOf(
                listOf("Entity", "@Entity", "Клас → таблиця SQLite"),
                listOf("DAO", "@Dao + @Query/@Insert/@Update/@Delete", "Методи читання й запису"),
                listOf("Database", "@Database", "Абстрактний клас; singleton; дає доступ до DAO"),
            ),
        )
        Tip("Room — типобезпечна обгортка над SQLite, а не окрема база. Запити перевіряються на етапі компіляції.")
    }

    DemoSection("Entity із слайда 5 — у цьому проєкті", note = "Файл data/Entities.kt") {
        CodeSnippet(
            """
            @Entity(
                tableName = "products",
                indices = [Index(value = ["product_name"])],
            )
            data class ProductEntity(
                @PrimaryKey val id: Int,
                @ColumnInfo(name = "product_name") val name: String,
                val price: Double,
                val cachedAt: Long = System.currentTimeMillis(),
                val updatedAt: Long = 0L,
                @Embedded(prefix = "sup_") val supplier: Supplier,
            ) {
                @Ignore var uiHint: String = ""
            }
            """,
        )
        CompareTable(
            listOf("Анотація", "Призначення"),
            listOf(
                listOf("@Entity", "Позначає клас як таблицю SQLite"),
                listOf("@PrimaryKey", "Первинний ключ рядка (обов'язково)"),
                listOf("@ColumnInfo", "Ім'я стовпця, відмінне від імені поля"),
                listOf("@Ignore", "Виключає поле зі схеми таблиці"),
                listOf("@Embedded", "Вкладає поля іншого об'єкта як стовпці"),
                listOf("@Index", "Індекс для прискорення пошуку"),
            ),
        )
    }

    DemoSection(
        "Що Room насправді створив у SQLite",
        note = "Це реальні запити до sqlite_master та PRAGMA table_info — Room згенерував CREATE TABLE із класу.",
    ) {
        tableSql.forEach { row -> CodeSnippet(row.getOrElse(1) { "" }.replace(", `", ",\n `")) }
        CompareTable(
            listOf("cid", "стовпець", "тип", "NOT NULL", "default", "PK"),
            columns,
        )
        Tip(
            "Зверніть увагу: product_name (з @ColumnInfo), sup_title і sup_city (з @Embedded), " +
                "а uiHint (@Ignore) у таблиці немає.",
        )
    }

    DemoSection("Спробуйте самі") {
        ActionRow(
            listOf(
                "Вставити 3 зразки й показати сирі рядки" to {
                    scope.launch {
                        dao.insertAll(sampleEntities())
                        rawRows = db.rawQuery(
                            "SELECT id, product_name, price, sup_title, sup_city FROM products ORDER BY id",
                        )
                    }
                },
                "Перевірити @Ignore" to {
                    scope.launch {
                        val entity = ProductEntity(900, "Тест @Ignore", 1.0).apply { uiHint = "тимчасова підказка" }
                        dao.insertReplace(entity)
                        val back = dao.getById(900)
                        ignoreResult = "Записали uiHint = '${entity.uiHint}', прочитали uiHint = '${back?.uiHint}' " +
                            "(порожньо: поля немає в таблиці)"
                    }
                },
            ),
            primaryFirst = true,
        )
        if (rawRows.isNotEmpty()) {
            CompareTable(listOf("id", "product_name", "price", "sup_title", "sup_city"), rawRows)
        }
        if (ignoreResult.isNotEmpty()) Text(ignoreResult)
    }

    DemoSection("Entity ≠ доменна модель") {
        CodeSnippet(
            """
            // domain/Product.kt — нічого не знає про Room
            data class Product(val id: Int, val name: String, val price: Double, ...)

            // data/Entities.kt — Mapper між шарами
            fun ProductEntity.toDomain() = Product(id, name, price, updatedAt, supplier.title, supplier.city)
            fun Product.toEntity(cachedAt: Long) = ProductEntity(...)
            """,
        )
        Tip(
            "Передавати Entity напряму в UI — порушення шарування Clean Architecture; при зміні таблиці доведеться " +
                "змінювати й екрани.",
            TipKind.Warning,
        )
    }
}
