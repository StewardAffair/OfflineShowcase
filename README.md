# Offline Showcase — тестовий застосунок до Лекції 18

Курс «Мобільна розробка: Android + Kotlin Multiplatform».
**Лекція 18 «Room, Offline-First, Синхронізація»** — кожен слайд має живий екран. Угорі кожного екрана є картка: *яка лекція → який слайд → що саме натиснути*.

**Що тут справжнє, а що симульоване**

| Справжнє | Симульоване |
|---|---|
| Room (Entity, DAO, Database, Flow, `@Embedded`, `@Ignore`, `@Index`, `OnConflictStrategy`, транзакції) | «Сервер» (`FakeApi`) — мережу можна вимкнути перемикачем |
| Міграції Room v1 → v2 на реальному файлі БД | Годинник можна «перемотати» на +31 хв (кеш 30 хв) |
| DataStore (Preferences) | Фонова синхронізація (WorkManager) — лише таймер |
| in-memory Room-тести | |

Інтернет, акаунти й ключі не потрібні.

## Запуск

1. Android Studio Ladybug (2024.2) або новіша, JDK 17.
2. **File → Open** → папка `OfflineShowcase`; дочекайтесь Gradle Sync (потрібен інтернет для залежностей; Room збирається через KSP).
3. Запустіть модуль `app` (minSdk 24).

> У проєкті є `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.11.1), але немає `gradlew`/`gradle-wrapper.jar`. Android Studio сама завантажить потрібний Gradle; з терміналу: `gradle wrapper --gradle-version 8.11.1` один раз.
>
> Проєкт написано без доступу до Maven/Android SDK, тому **не компілювався**. Якщо Gradle або Kotlin покажуть помилку — надішліть її текст, і я виправлю.

## Слайд → екран → файл

| Слайд | Тема | Екран | Файл (`l18/`) |
|---|---|---|---|
| 2–3 | Чому локальне сховище; план лекції | Чому локальне сховище | `WhyLocalDemo.kt` |
| 4–5 | Три компоненти Room; Entity | Room: Entity | `EntityDemo.kt` |
| 6 | DAO; `OnConflictStrategy` | Room: DAO | `DaoDemo.kt` |
| 7 | Flow з DAO | Flow з DAO | `FlowDemo.kt` |
| 8 | Room і корутини | Room та корутини | `CoroutinesDemo.kt` |
| 9 | Міграції | Міграції | `MigrationDemo.kt`, `data/MigrationLab.kt` |
| 10, 15 | Single source of truth; архітектурна діаграма | Offline-First | `SsotDemo.kt` |
| 11 | Стратегії синхронізації | Стратегії синхронізації | `SyncStrategyDemo.kt` |
| 12 | Обробка конфліктів | Обробка конфліктів | `ConflictDemo.kt`, `domain/Sync.kt` |
| 13 | Graceful degradation | Graceful degradation | `GracefulDemo.kt` |
| 14 | Room vs DataStore | Room vs DataStore | `RoomVsDataStoreDemo.kt`, `data/SettingsStore.kt` |
| 16 | Типові помилки | Типові помилки з Room | `PitfallsDemo.kt` |
| 17 | Практика: схема для власного проєкту | Практика | `PracticeDemo.kt`, `domain/CodeGen.kt` |
| 18 | Тестування Room (in-memory) | Тестування Room | `TestingDemo.kt`, `data/DaoChecks.kt` |
| 19–20 | Підсумок, питання, домашнє | Підсумок | `SummaryDemo.kt` |

Номери слайдів — за наданим PDF (20 слайдів).

## Що цікавого варто показати студентам

- **Екран «Entity»**: Room сам створив `CREATE TABLE` — видно `product_name` (`@ColumnInfo`), `sup_title/sup_city` (`@Embedded`), а `uiHint` (`@Ignore`) у таблиці немає.
- **Екран «Корутини»**: перша кнопка справді викликає DAO з UI-потоку й показує `IllegalStateException`.
- **Екран «Міграції»**: база v1 створюється й відкривається як v2 трьома способами (з міграцією — дані живі; без — виняток; destructive — дані втрачено).
- **Екран «Offline-First»**: Room (що бачить UI) і «сервер» розходяться, поки не зроблено `refresh()`. Вимкніть «Атомарну заміну» — `deleteAll()` + `insertAll()` дадуть окремий порожній emit («моргання» списку).
- **Екран «Конфлікти»**: сценарій 0→A→B→C→D у двох порядках (B перед C і C перед B) для двох стратегій.
- **Екран «Типові помилки»**: автогенерований `PrimaryKey` + `REPLACE` → дублікати після кожного `refresh()`.

## Структура

```
app/src/main/java/ua/ofkkt/offlineshowcase/
├── MainActivity.kt, AppContainer.kt   ручний DI (замість Hilt/Koin)
├── domain/    Product, Sync (shouldRefresh, resolveConflict), CodeGen — чистий Kotlin
├── data/      Entities, Daos, AppDatabase, MigrationLab, SettingsStore (DataStore),
│              FakeApi, Clock, ProductRepository, DaoChecks
├── ui/        Тема, спільні компоненти, AppRoot (меню й екран демо)
└── l18/       15 екранів лекції + Catalog.kt
app/src/test/         JUnit (JVM): SyncLogicTest, CodeGenTest, FakeApiTest
app/src/androidTest/  DaoInMemoryTest, MigrationLabTest, RepositoryOfflineFirstTest
```

## Тести

- `./gradlew testDebugUnitTest` — швидкі тести без емулятора (логіка конфліктів і кешу, генератор коду, `FakeApi`).
- `./gradlew connectedDebugAndroidTest` — на емуляторі/пристрої: DAO на in-memory Room (дослівно тест зі слайда 18), три сценарії міграції, `Repository` + Room (offline, конфлікти).

## Зауваження до слайдів

- Слайд 6 показує `ProductDao` як `interface` — так і зроблено. Атомарну заміну (`deleteAll` + `insertAll`) винесено в `db.withTransaction { }` у Repository, бо `@Transaction` із тілом в інтерфейсі вимагає окремих налаштувань.
- Слайд 5 використовує `Int` як `id`; тут додано `updatedAt` (для timestamp-стратегії) і `@Embedded Supplier`, `@Ignore` — щоб показати всі анотації з таблиці слайда.
- Слайд 11: `shouldRefresh()` винесено в чисту функцію `domain/Sync.kt`, час останньої синхронізації зберігається в DataStore (слайд 14). У лекції це `prefs.saveLastSyncTime(...)`.
- Слайд 12: Merge/CRDT лише описано (лекція теж виводить його за межі курсу). Реалізовано Last-Write-Wins і Timestamp-based; у переможній локальної версії Repository ще й надсилає її на «сервер».
- Слайд 11: WorkManager не підключено; фон імітується таймером і показано код реального `PeriodicWorkRequest`.
- Демо-екрани для Entity/DAO/Flow/корутин/помилок працюють на **окремій in-memory базі**, щоб не псувати кеш, який використовують екрани Offline-First, Синхронізація, Конфлікти й Graceful degradation. Справжній файл `app_database` зберігається між запусками — це і є offline-first.
- `fallbackToDestructiveMigration()` в `AppDatabase` — лише для розробки (слайд 9).
- Слайд 17 згадує **Лаб5**, слайд 19 — **Лекцію 19** (Flow/StateFlow): посилання залишено як у слайдах.
