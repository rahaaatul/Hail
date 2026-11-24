package com.aistra.hail.app

import android.content.Context
import androidx.room3.Room
import com.aistra.hail.HailApp
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.AppMetadataDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class UninstallReinstallTest {
    private lateinit var context: Context
    private lateinit var db: AppMetadataDatabase

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as HailApp
        HailApp.setAppForTest(app)
        context = app
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppMetadataDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        AppMetaCache.useDatabaseForTest(db)
        HailDataStore.resetForTest()
    }

    @After
    fun tearDown() {
        AppMetaCache.useDatabaseForTest(null)
        db.close()
    }

    @Test
    fun `checked app survives a metadata rebuild`() =
        runBlocking {
            HailData.addCheckedApp("com.example.pkg", tagId = 1)
            HailData.flush()
            assertEquals(1, db.checkedAppDao().loadAll().size)

            AppMetaCache.clearAndRebuild().join()

            // Rebuilding app_metadata must not touch checked_apps: the checked/tag state is the
            // restore state and has to survive an uninstall/reinstall.
            assertTrue(HailData.isChecked("com.example.pkg"))
            assertEquals(1, db.checkedAppDao().loadAll().size)
            assertTrue(db.appMetadataDao().loadAll().isNotEmpty())
        }

    @Test
    fun `checked app stays checked across a snapshot reload`() =
        runBlocking {
            HailData.addCheckedApp("com.example.pkg", tagId = 1)
            HailData.flush()

            HailDataStore.resetSnapshotForTest()

            assertTrue(HailData.isChecked("com.example.pkg"))
        }
}
