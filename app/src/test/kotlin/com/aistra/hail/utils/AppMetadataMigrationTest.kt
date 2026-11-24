package com.aistra.hail.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import com.aistra.hail.HailApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class AppMetadataMigrationTest {
    private lateinit var context: Context
    private lateinit var file: File

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as HailApp
        HailApp.setAppForTest(app)
        context = app
        file = File(context.filesDir, "migration-test.db")
        file.delete()
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun `migration 6 to 7 adds the new tables and keeps app metadata`() {
        // `exportSchema = false` leaves no schema files for MigrationTestHelper.createDatabase, and
        // KSP is not applied to test sources, so the v6 database is created by hand. The version is
        // stamped into user_version so Room treats the file as a v6 database and runs the migration.
        SQLiteDatabase
            .openOrCreateDatabase(file.path, null)
            .apply {
                execSQL(
                    "CREATE TABLE app_metadata (" +
                        "packageName TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "systemApp INTEGER NOT NULL, " +
                        "firstInstallTime INTEGER NOT NULL, " +
                        "lastUpdateTime INTEGER NOT NULL, " +
                        "flags INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL, " +
                        "installed INTEGER NOT NULL, " +
                        "frozen INTEGER NOT NULL DEFAULT 0, " +
                        "sourceSignature TEXT NOT NULL, " +
                        "PRIMARY KEY(packageName))",
                )
                execSQL(
                    "INSERT INTO app_metadata (packageName, name, systemApp, firstInstallTime, " +
                        "lastUpdateTime, flags, enabled, installed, frozen, sourceSignature) " +
                        "VALUES ('com.example.old', 'Old', 0, 0, 0, 0, 1, 1, 0, '')",
                )
                // The v6 schema already has the action tables (added in 2 -> 3); v7 validates every
                // entity, so the fixture must be complete.
                execSQL(
                    "CREATE TABLE IF NOT EXISTS actions (" +
                        "id TEXT NOT NULL, launchPackage TEXT NOT NULL, PRIMARY KEY(id))",
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS action_dependencies (" +
                        "actionId TEXT NOT NULL, packageName TEXT NOT NULL, ordering INTEGER NOT NULL, " +
                        "PRIMARY KEY(actionId, packageName), " +
                        "FOREIGN KEY(actionId) REFERENCES actions(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                version = 6
                close()
            }

        val db =
            Room
                .databaseBuilder(context, AppMetadataDatabase::class.java, file.absolutePath)
                .addMigrations(AppMetaCache.MIGRATION_6_7)
                // Robolectric runs the test body on the main thread, so Room's main-thread guard
                // would reject every blocking DAO call below.
                .allowMainThreadQueries()
                .build()

        val migrated = db.appMetadataDao().loadAll()
        assertEquals(1, migrated.size)
        assertEquals("com.example.old", migrated[0].packageName)
        assertEquals("Old", migrated[0].name)

        db.checkedAppDao().upsertAll(listOf(CheckedAppEntity("com.example.new", pinned = true)))
        val checked = db.checkedAppDao().loadAll()
        assertEquals(1, checked.size)
        assertTrue(checked[0].pinned)

        db.tagDao().upsertAll(listOf(TagEntity(id = 2, name = "Work")))
        assertEquals(listOf("Work"), db.tagDao().loadAll().map { it.name })

        // The JSON import marker does not exist until the first import runs.
        assertNull(db.stateFlagDao().get("json_imported"))
        db.close()
    }
}
