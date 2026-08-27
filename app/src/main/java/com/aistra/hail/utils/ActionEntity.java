package com.aistra.hail.utils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "actions")
public class ActionEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String launchPackage = "";
}