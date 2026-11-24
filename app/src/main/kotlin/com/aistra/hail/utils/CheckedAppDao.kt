package com.aistra.hail.utils

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert

@Dao
interface CheckedAppDao {
    @Query("SELECT * FROM checked_apps")
    fun loadAll(): List<CheckedAppEntity>

    @Upsert
    fun upsertAll(apps: List<CheckedAppEntity>)

    @Query("DELETE FROM checked_apps")
    fun deleteAll()

    @Transaction
    fun replaceAll(apps: List<CheckedAppEntity>) {
        deleteAll()
        upsertAll(apps)
    }
}
