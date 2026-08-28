package com.aistra.hail.utils;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {AppMetadataEntity.class, ActionEntity.class, ActionDependencyEntity.class},
        version = 4,
        exportSchema = false
)
public abstract class AppMetadataDatabase extends RoomDatabase {
    public abstract AppMetadataDao appMetadataDao();

    public abstract ActionDao actionDao();
}
