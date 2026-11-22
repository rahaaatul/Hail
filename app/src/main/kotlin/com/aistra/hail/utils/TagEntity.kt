package com.aistra.hail.utils

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: Int,
    val name: String,
)
