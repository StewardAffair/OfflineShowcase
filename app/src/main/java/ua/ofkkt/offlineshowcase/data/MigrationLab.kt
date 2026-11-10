package ua.ofkkt.offlineshowcase.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---- Версія 1 схеми: products(id, name) ----
@Entity(tableName = "products")
data class LegacyProductV1(
    @PrimaryKey val id: Int,
    val name: String,
)

@Dao
interface LegacyDaoV1 {
    @Insert
    suspend fun insertAll(items: List<LegacyProductV1>)

    @Query("SELECT * FROM products ORDER BY id")
    suspend fun getAll(): List<LegacyProductV1>
}

@Database(entities = [LegacyProductV1::class], version = 1, exportSchema = false)
abstract class LegacyDbV1 : RoomDatabase() {
    abstract fun dao(): LegacyDaoV1
}

// ---- Версія 2 схеми: додано стовпець price (слайд 9) ----
@Entity(tableName = "products")
data class LegacyProductV2(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(defaultValue = "0.0") val price: Double = 0.0,
)

@Dao
interface LegacyDaoV2 {
    @Query("SELECT * FROM products ORDER BY id")
    suspend fun getAll(): List<LegacyProductV2>
}

@Database(entities = [LegacyProductV2::class], version = 2, exportSchema = false)
abstract class LegacyDbV2 : RoomDatabase() {
    abstract fun dao(): LegacyDaoV2
}

/** Слайд 9: точно та міграція, що в лекції. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN price REAL NOT NULL DEFAULT 0.0")
    }
}

enum class MigrationMode(val label: String) {
    WithMigration(".addMigrations(MIGRATION_1_2)"),
    NoMigration("Без міграції"),
    Destructive(".fallbackToDestructiveMigration()"),
}

data class MigrationReport(
    val before: List<String>,
    val after: List<String>?,
    val error: String?,
)

/**
 * Справжній експеримент: створює файл БД версії 1, наповнює його, а потім відкриває той самий
 * файл як версію 2 — трьома способами. Усе відбувається у фоновому потоці.
 */
class MigrationLab(context: Context) {
    private val appContext = context.applicationContext

    suspend fun run(mode: MigrationMode): MigrationReport = withContext(Dispatchers.IO) {
        appContext.deleteDatabase(DB_NAME)

        val v1 = Room.databaseBuilder(appContext, LegacyDbV1::class.java, DB_NAME).build()
        val before = try {
            v1.dao().insertAll(
                listOf(
                    LegacyProductV1(1, "Яблука"),
                    LegacyProductV1(2, "Молоко"),
                    LegacyProductV1(3, "Хліб"),
                ),
            )
            v1.dao().getAll().map { "#${it.id} ${it.name}" }
        } finally {
            v1.close()
        }

        val builder = Room.databaseBuilder(appContext, LegacyDbV2::class.java, DB_NAME)
        when (mode) {
            MigrationMode.WithMigration -> builder.addMigrations(MIGRATION_1_2)
            MigrationMode.NoMigration -> Unit
            MigrationMode.Destructive -> builder.fallbackToDestructiveMigration()
        }
        val v2 = builder.build()
        try {
            val after = v2.dao().getAll().map { "#${it.id} ${it.name} · ціна ${it.price}" }
            MigrationReport(before, after, null)
        } catch (e: Exception) {
            MigrationReport(before, null, "${e::class.java.simpleName}: ${e.message}")
        } finally {
            v2.close()
            appContext.deleteDatabase(DB_NAME)
        }
    }

    private companion object {
        const val DB_NAME = "migration_lab.db"
    }
}
