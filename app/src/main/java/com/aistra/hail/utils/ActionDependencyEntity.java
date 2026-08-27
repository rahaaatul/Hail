package com.aistra.hail.utils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "action_dependencies",
        primaryKeys = {"actionId", "packageName"},
        foreignKeys = @ForeignKey(
                entity = ActionEntity.class,
                parentColumns = "id",
                childColumns = "actionId",
                onDelete = ForeignKey.CASCADE
        )
)
public class ActionDependencyEntity {
    @NonNull
    public String actionId = "";
    @NonNull
    public String packageName = "";
    public int position;
}