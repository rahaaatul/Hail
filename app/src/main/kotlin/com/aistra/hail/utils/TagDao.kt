package com.aistra.hail.utils

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY id")
    fun loadAll(): List<TagEntity>

    @Upsert
    fun upsertAll(tags: List<TagEntity>)

    @Query("DELETE FROM tags")
    fun deleteAll()

    @Transaction
    fun replaceAll(tags: List<TagEntity>) {
        deleteAll()
        upsertAll(tags)
    }
}
