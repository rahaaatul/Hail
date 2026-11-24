package com.aistra.hail.utils

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "checked_apps")
data class CheckedAppEntity(
    @PrimaryKey val packageName: String,
    val pinned: Boolean = false,
    val whitelisted: Boolean = false,
    val tagIds: String = "",
)
