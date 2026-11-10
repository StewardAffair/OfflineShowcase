package ua.ofkkt.offlineshowcase.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.ofkkt.offlineshowcase.l18.Demos

/** Навігація навмисно проста: один стан «який екран відкрито» + системна кнопка «Назад». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    var currentId by rememberSaveable { mutableStateOf<String?>(null) }
    val current = Demos.all.firstOrNull { it.id == currentId }

    BackHandler(enabled = current != null) { currentId = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.title ?: "Лекція 18 · Room, Offline-First") },
                navigationIcon = {
                    if (current != null) {
                        IconButton(onClick = { currentId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Hub(Modifier.padding(padding), onOpen = { currentId = it })
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LectureCard("Лекція 18", "слайд ${current.slides}", current.tryIt)
                current.content()
            }
        }
    }
}

@Composable
private fun Hub(modifier: Modifier, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Room, Offline-First, Синхронізація",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Тут працюють справжні Room і DataStore; сервер симульований, тому мережу можна вимикати кнопкою. " +
                    "Кожен екран — один розділ лекції. Вгорі кожного екрана написано, який це слайд і що спробувати.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(Demos.all, key = { it.id }) { demo ->
            Card(
                Modifier.fillMaxWidth().clickable { onOpen(demo.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(demo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Слайд ${demo.slides}", style = MaterialTheme.typography.labelMedium)
                    Text(demo.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
