package ua.ofkkt.offlineshowcase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ua.ofkkt.offlineshowcase.data.MigrationLab
import ua.ofkkt.offlineshowcase.data.MigrationMode

/** Слайд 9: три способи відкрити базу v1 як v2. */
@RunWith(AndroidJUnit4::class)
class MigrationLabTest {

    private val lab = MigrationLab(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun migrationKeepsData() = runTest {
        val report = lab.run(MigrationMode.WithMigration)
        assertNull(report.error)
        assertEquals(3, report.after?.size)
        assertTrue(report.after!!.all { it.contains("ціна 0.0") })
    }

    @Test
    fun noMigrationFails() = runTest {
        val report = lab.run(MigrationMode.NoMigration)
        assertNotNull(report.error)
        assertTrue(report.error!!.contains("IllegalStateException"))
    }

    @Test
    fun destructiveLosesData() = runTest {
        val report = lab.run(MigrationMode.Destructive)
        assertNull(report.error)
        assertEquals(0, report.after?.size)
    }
}
