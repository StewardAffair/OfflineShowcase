package ua.ofkkt.offlineshowcase.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Слайд 4: Database об'єднує всі Entity та надає доступ до DAO. */
@Database(
    entities = [ProductEntity::class, NaiveProductEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun naiveDao(): NaiveDao

    companion object {
        /** Справжня база: файл app_database. Singleton створюється в AppContainer. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                // Лише для розробки (слайд 9)! У production потрібні справжні міграції.
                .fallbackToDestructiveMigration()
                .build()

        /** Слайд 18: in-memory база для тестів — зникає після close(). */
        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }
}
