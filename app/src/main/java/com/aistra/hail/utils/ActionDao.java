package com.aistra.hail.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ActionDao {
    @Query("SELECT * FROM actions ORDER BY rowid")
    List<ActionEntity> loadAll();

    @Query("SELECT * FROM action_dependencies WHERE actionId = :actionId ORDER BY position")
    List<ActionDependencyEntity> loadDependencies(String actionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ActionEntity action);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertDependencies(List<ActionDependencyEntity> dependencies);

    @Query("DELETE FROM action_dependencies WHERE actionId = :actionId")
    void deleteDependencies(String actionId);

    @Query("DELETE FROM actions WHERE id = :actionId")
    void delete(String actionId);

    @androidx.room.Transaction
    default void saveAction(ActionEntity action, List<ActionDependencyEntity> dependencies) {
        upsert(action);
        deleteDependencies(action.id);
        upsertDependencies(dependencies);
    }
}