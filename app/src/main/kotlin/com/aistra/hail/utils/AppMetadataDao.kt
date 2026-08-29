package com.aistra.hail.utils

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface AppMetadataDao {
    @Query("SELECT * FROM app_metadata")
    fun loadAll(): List<AppMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entries: List<AppMetadataEntity>)

    @Query("DELETE FROM app_metadata")
    fun deleteAll()

    @Query("UPDATE app_metadata SET installed = 0")
    fun markAllUninstalled()

    @Transaction
    fun replaceAll(entries: List<AppMetadataEntity>) {
        deleteAll()
        upsertAll(entries)
    }
}
