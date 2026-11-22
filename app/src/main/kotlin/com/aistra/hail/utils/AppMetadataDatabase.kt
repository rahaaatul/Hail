package com.aistra.hail.utils

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [
        AppMetadataEntity::class,
        ActionEntity::class,
        ActionDependencyEntity::class,
        CheckedAppEntity::class,
        TagEntity::class,
        StateFlagEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppMetadataDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao

    abstract fun actionDao(): ActionDao

    abstract fun checkedAppDao(): CheckedAppDao

    abstract fun tagDao(): TagDao

    abstract fun stateFlagDao(): StateFlagDao
}
