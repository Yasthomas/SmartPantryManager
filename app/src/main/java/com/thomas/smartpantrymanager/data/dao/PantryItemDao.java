package com.thomas.smartpantrymanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.thomas.smartpantrymanager.data.entity.PantryItem;

import java.util.List;

/**
 * All pantry CRUD.
 *
 * The LiveData query is the single source of truth for the list screen: Room re-emits
 * automatically after any insert/update/delete, so the UI never has to refresh itself.
 * The synchronous twins are only ever called from the application executor.
 */
@Dao
public interface PantryItemDao {

    @Insert
    long insert(PantryItem item);

    @Update
    void update(PantryItem item);

    @Delete
    void delete(PantryItem item);

    /** Case-insensitive alphabetical order so "salt" and "Salt" sit next to each other. */
    @Query("SELECT * FROM pantry_items ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<PantryItem>> getAllItems();

    /** Same ordering, blocking variant: background threads only (matching, seeding). */
    @Query("SELECT * FROM pantry_items ORDER BY name COLLATE NOCASE ASC")
    List<PantryItem> getAllItemsSync();

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    PantryItem getById(long id);

    /** Used by "Delete all pantry items" in Settings; a single statement keeps it atomic. */
    @Query("DELETE FROM pantry_items")
    void deleteAll();
}
