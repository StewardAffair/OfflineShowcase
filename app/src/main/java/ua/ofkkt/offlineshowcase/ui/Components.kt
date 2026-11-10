@file:OptIn(ExperimentalLayoutApi::class)

package ua.ofkkt.offlineshowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Секція демо: заголовок + необов'язкова примітка + вміст. */
@Composable
fun DemoSection(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

/** Моноширинний блок коду (виділяється й копіюється). */
@Composable
fun CodeSnippet(code: String, modifier: Modifier = Modifier) {
    SelectionContainer {
        Box(
            modifier
                .fillMaxWidth()
                .background(Color(0xFF263238), RoundedCornerShape(8.dp))
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
        ) {
            Text(
                code.trimIndent(),
                color = Color(0xFFECEFF1),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

enum class TipKind { Info, Warning, Success, Error }

@Composable
fun Tip(text: String, kind: TipKind = TipKind.Info, modifier: Modifier = Modifier) {
    val (bg, mark) = when (kind) {
        TipKind.Info -> Color(0xFFE3F2FD) to "ℹ️"
        TipKind.Warning -> Color(0xFFFFF8E1) to "⚠️"
        TipKind.Success -> Color(0xFFE8F5E9) to "✅"
        TipKind.Error -> Color(0xFFFFEBEE) to "❌"
    }
    Box(modifier.fillMaxWidth().background(bg, RoundedCornerShape(8.dp)).padding(10.dp)) {
        Text("$mark  $text", color = Color(0xFF1B1B1B), style = MaterialTheme.typography.bodySmall)
    }
}

/** Проста таблиця порівняння: перший рядок — заголовки. */
@Composable
fun CompareTable(headers: List<String>, rows: List<List<String>>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outline
    Column(modifier.fillMaxWidth().border(1.dp, line, RoundedCornerShape(6.dp))) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(6.dp)) {
            headers.forEach {
                Text(it, Modifier.weight(1f).padding(2.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().border(0.5.dp, line).padding(6.dp)) {
                row.forEach { cell -> Text(cell, Modifier.weight(1f).padding(2.dp), fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
}

/** Ряд кнопок, що переноситься на новий рядок. Кожна пара — (текст, дія). */
@Composable
fun ActionRow(actions: List<Pair<String, () -> Any?>>, primaryFirst: Boolean = false) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        actions.forEachIndexed { index, (text, action) ->
            if (primaryFirst && index == 0) PrimaryButton(text, { action() }) else SecondaryButton(text, { action() })
        }
    }
}

/** Журнал подій: новіші записи — зверху. */
class EventLog(private val max: Int = 40) {
    val lines: SnapshotStateList<String> = mutableStateListOf()

    fun add(line: String) {
        lines.add(0, line)
        while (lines.size > max) lines.removeAt(lines.lastIndex)
    }

    fun clear() = lines.clear()
}

@Composable
fun LogView(log: EventLog, title: String = "Журнал", height: Dp = 140.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp, max = height)
                .background(Color(0xFF111111), RoundedCornerShape(6.dp))
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            if (log.lines.isEmpty()) {
                Text("(порожньо)", color = Color(0xFF888888), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            } else {
                Column {
                    log.lines.forEach {
                        Text(it, color = Color(0xFFB9F6CA), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** Картка угорі кожного екрана: яка лекція, які слайди, що спробувати. */
@Composable
fun LectureCard(lecture: String, slides: String, tryIt: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📖 $lecture · $slides", fontWeight = FontWeight.Bold)
            Text("👉 Що спробувати: $tryIt", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
