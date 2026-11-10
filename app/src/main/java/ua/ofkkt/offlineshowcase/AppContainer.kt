package ua.ofkkt.offlineshowcase

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import ua.ofkkt.offlineshowcase.data.AppDatabase
import ua.ofkkt.offlineshowcase.data.Clock
import ua.ofkkt.offlineshowcase.data.FakeApi
import ua.ofkkt.offlineshowcase.data.MigrationLab
import ua.ofkkt.offlineshowcase.data.ProductRepository
import ua.ofkkt.offlineshowcase.data.SettingsStore
import ua.ofkkt.offlineshowcase.data.settingsDataStore

/** Ручний DI: усі залежності створюються тут один раз (Room — singleton, слайд 4 і 8). */
class AppContainer(context: Context) {
    val clock = Clock()
    val api = FakeApi(clock)
    val database: AppDatabase = AppDatabase.create(context)
    val settings = SettingsStore(context.settingsDataStore)
    val repository = ProductRepository(database, api, settings, clock)
    val migrationLab = MigrationLab(context)
}

class ShowcaseApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

@Composable
fun rememberContainer(): AppContainer =
    (LocalContext.current.applicationContext as ShowcaseApp).container
