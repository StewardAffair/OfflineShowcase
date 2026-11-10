package ua.ofkkt.offlineshowcase.l18

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.ofkkt.offlineshowcase.data.AppDatabase
import ua.ofkkt.offlineshowcase.data.ProductEntity
import ua.ofkkt.offlineshowcase.data.Supplier
import ua.ofkkt.offlineshowcase.domain.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun uah(value: Double): String = String.format(Locale.US, "%.2f ₴", value)

fun clockTime(ms: Long): String =
    if (ms <= 0L) "—" else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ms))

/** Окрема in-memory база для екрана: не чіпає справжній кеш і зникає разом з екраном (слайд 18). */
@Composable
fun rememberScratchDb(): AppDatabase {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.inMemory(context) }
    DisposableEffect(db) { onDispose { db.close() } }
    return db
}

fun sampleEntities(): List<ProductEntity> = listOf(
    ProductEntity(1, "Яблука", 42.0, supplier = Supplier("ФОП Коваль", "Умань")),
    ProductEntity(2, "Молоко", 38.5, supplier = Supplier("Ферма «Зоря»", "Полтава")),
    ProductEntity(3, "Хліб", 27.0, supplier = Supplier("Пекарня №3", "Одеса")),
)

/** Виконує довільний SELECT/PRAGMA і повертає рядки як текст (для показу «що насправді в SQLite»). */
suspend fun AppDatabase.rawQuery(sql: String): List<List<String>> = withContext(Dispatchers.IO) {
    openHelper.readableDatabase.query(sql).use { cursor ->
        val rows = mutableListOf<List<String>>()
        while (cursor.moveToNext()) {
            rows += (0 until cursor.columnCount).map { index -> cursor.getString(index) ?: "NULL" }
        }
        rows
    }
}

fun describe(e: Throwable): String = "${e::class.java.simpleName}: ${e.message}"

@Composable
fun ProductRows(items: List<Product>, emptyText: String = "Порожньо") {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (items.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall)
        }
        items.forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("#${it.id} ${it.name}", style = MaterialTheme.typography.bodyMedium)
                Text(uah(it.price), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun StatusPill(text: String, good: Boolean) {
    val bg = if (good) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
    Text(
        text,
        color = Color(0xFF1B1B1B),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Список із позначками; стан зберігається як рядок «0,2,3» (щоб пережити поворот екрана). */
@Composable
fun CheckList(items: List<String>) {
    var doneRaw by rememberSaveable { mutableStateOf("") }
    val done = doneRaw.split(',').filter { it.isNotEmpty() }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { index, text ->
            val key = index.toString()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = key in done,
                    onCheckedChange = { checked ->
                        val next = if (checked) done + key else done - key
                        doneRaw = next.joinToString(",")
                    },
                )
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("Виконано: ${done.size} з ${items.size}", style = MaterialTheme.typography.labelMedium)
    }
}
