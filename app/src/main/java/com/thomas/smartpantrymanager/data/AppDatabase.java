package com.thomas.smartpantrymanager.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.thomas.smartpantrymanager.data.dao.PantryItemDao;
import com.thomas.smartpantrymanager.data.dao.RecipeDao;
import com.thomas.smartpantrymanager.data.entity.PantryItem;
import com.thomas.smartpantrymanager.data.entity.Recipe;
import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;

/**
 * The single Room database for the app.
 *
 * Deliberately no {@code allowMainThreadQueries()}: every call goes through the executor
 * owned by {@code PantryApp}, so a slow query can never jank or crash the UI thread.
 * exportSchema is off because the schema is version 1 and ships with the app.
 */
@Database(
        entities = {PantryItem.class, Recipe.class, RecipeIngredient.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase sInstance;

    public abstract PantryItemDao pantryItemDao();

    public abstract RecipeDao recipeDao();

    public static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smart_pantry.db"
                    ).build();
                }
            }
        }
        return sInstance;
    }
}
