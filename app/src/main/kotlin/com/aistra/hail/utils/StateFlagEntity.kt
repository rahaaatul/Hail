package com.aistra.hail.utils

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "state_flags")
data class StateFlagEntity(
    @PrimaryKey val key: String,
    val value: Int = 0,
)
