package com.aistra.hail.utils

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface StateFlagDao {
    @Query("SELECT value FROM state_flags WHERE key = :key")
    fun get(key: String): Int?

    @Upsert
    fun set(flag: StateFlagEntity)
}
