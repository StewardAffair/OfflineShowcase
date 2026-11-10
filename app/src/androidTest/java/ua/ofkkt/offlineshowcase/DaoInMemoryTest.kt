package ua.ofkkt.offlineshowcase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ua.ofkkt.offlineshowcase.data.AppDatabase
import ua.ofkkt.offlineshowcase.data.DaoChecks
import ua.ofkkt.offlineshowcase.data.ProductDao
import ua.ofkkt.offlineshowcase.data.ProductEntity

/** Слайд 18: тест DAO на in-memory базі (дослівно за лекцією). Запуск: ./gradlew connectedDebugAndroidTest */
@RunWith(AndroidJUnit4::class)
class DaoInMemoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProductDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.productDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAll() = runTest {
        val entity = ProductEntity(id = 1, name = "Test", price = 9.99)
        dao.insertAll(listOf(entity))
        val result = dao.getAll().first()
        assertEquals(1, result.size)
        assertEquals("Test", result[0].name)
    }

    @Test
    fun allInAppChecksPass() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val results = DaoChecks.runAll(context)
        assertTrue(results.filterNot { it.passed }.joinToString { "${it.name}: ${it.message}" }, results.all { it.passed })
    }
}
