package ua.ofkkt.offlineshowcase.l18

import androidx.compose.runtime.Composable

class Demo(
    val id: String,
    val title: String,
    /** Номери слайдів Лекції 18. */
    val slides: String,
    val subtitle: String,
    val tryIt: String,
    val content: @Composable () -> Unit,
)

object Demos {
    val all: List<Demo> = listOf(
        Demo(
            "why", "Чому локальне сховище", "2–3",
            "Мережа ненадійна: екран із мережі vs екран із Room. План лекції.",
            "Синхронізуйтесь, вимкніть мережу й порівняйте варіанти А і Б.",
        ) { WhyLocalDemo() },
        Demo(
            "entity", "Room: Entity", "4–5",
            "Три компоненти Room; анотації Entity; SQL, який Room створив сам.",
            "Подивіться CREATE TABLE і стовпці; перевірте, що @Ignore-поле не зберігається.",
        ) { EntityDemo() },
        Demo(
            "dao", "Room: DAO і OnConflictStrategy", "6",
            "@Query, @Insert, @Update, @Delete; REPLACE / IGNORE / ABORT.",
            "Заповніть таблицю й вставте дублікат з кожною стратегією.",
        ) { DaoDemo() },
        Demo(
            "flow", "Flow з DAO", "7",
            "Підписка один раз — UI оновлюється сам після будь-якого запису.",
            "Натискайте INSERT / UPDATE / DELETE й дивіться журнал emit-ів.",
        ) { FlowDemo() },
        Demo(
            "coroutines", "Room та корутини", "8",
            "suspend-запис, заборона запитів з UI-потоку.",
            "Натисніть «getAllBlocking() прямо з UI»; потім вставку 20 000 рядків.",
        ) { CoroutinesDemo() },
        Demo(
            "migration", "Міграції", "9",
            "Справжнє оновлення схеми v1 → v2 трьома способами.",
            "Запустіть кожен режим: з міграцією, без неї, destructive.",
        ) { MigrationDemo() },
        Demo(
            "ssot", "Offline-First: single source of truth", "10, 15",
            "Мережа → Repository → Room → Flow → UI; Room проти сервера.",
            "Зробіть зміну на «іншому пристрої» й подивіться, коли UI її побачить.",
        ) { SsotDemo() },
        Demo(
            "sync", "Стратегії синхронізації", "11",
            "Ручна, автоматична при вході (кеш 30 хв), фонова.",
            "Перемотайте час на +31 хв і відкрийте екран.",
        ) { SyncStrategyDemo() },
        Demo(
            "conflicts", "Обробка конфліктів", "12",
            "Last-Write-Wins, Timestamp-based, Merge/CRDT на живому сценарії.",
            "Пройдіть сценарій 0→A→B→C→D у двох порядках для обох стратегій.",
        ) { ConflictDemo() },
        Demo(
            "graceful", "Graceful degradation без мережі", "13",
            "Банер «Офлайн», кеш замість помилки, автоматичне відновлення.",
            "Вимкніть мережу, натисніть refresh(), потім увімкніть мережу.",
        ) { GracefulDemo() },
        Demo(
            "datastore", "Room vs DataStore", "14",
            "Порівняння та живий DataStore; тренажер «що де зберігати».",
            "Збережіть ім'я, перезапустіть застосунок; пройдіть тренажер.",
        ) { RoomVsDataStoreDemo() },
        Demo(
            "pitfalls", "Типові помилки з Room", "16",
            "Чотири помилки; відтворення дублікатів після refresh().",
            "Натисніть обидві кнопки refresh() по 3 рази й порівняйте кількість рядків.",
        ) { PitfallsDemo() },
        Demo(
            "practice", "Практика: схема для власного проєкту", "17",
            "Чекліст заняття та генератор Entity + DAO + Repository.",
            "Введіть свою сутність і поля — скопіюйте згенерований код.",
        ) { PracticeDemo() },
        Demo(
            "testing", "Тестування Room (in-memory)", "18",
            "5 DAO-тестів на in-memory базі — прямо в застосунку.",
            "Запустіть тести; прочитайте код тесту зі слайда.",
        ) { TestingDemo() },
        Demo(
            "summary", "Підсумок, питання й домашнє", "19–20",
            "Тези лекції, контрольні питання з відповідями, чекліст Лаб5.",
            "Спробуйте відповісти на питання, перш ніж показувати відповідь.",
        ) { SummaryDemo() },
    )
}
