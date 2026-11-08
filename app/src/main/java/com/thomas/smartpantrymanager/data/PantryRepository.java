package com.thomas.smartpantrymanager.data;

import androidx.lifecycle.LiveData;

import com.thomas.smartpantrymanager.PantryApp;
import com.thomas.smartpantrymanager.data.dao.PantryItemDao;
import com.thomas.smartpantrymanager.data.dao.RecipeDao;
import com.thomas.smartpantrymanager.data.entity.PantryItem;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;

import java.util.List;

/**
 * Application-scoped wrapper around the Room DAOs.
 *
 * Activities never see SQL: they call these methods and get either a LiveData stream
 * (for the pantry list, which is observed) or a callback delivered on the main thread
 * (for one-off reads and writes). All DAO work runs on the app executor, which is why
 * this class contains no Android UI code at all — it cannot, the executor thread has
 * no Looper and no view reference.
 */
public class PantryRepository {

    /** Result callback for one-off reads; always invoked on the main thread. */
    public interface Callback<T> {
        void onResult(T result);
    }

    private static volatile PantryRepository sInstance;

    private final PantryApp mApp;
    private final AppDatabase mDatabase;

    private PantryRepository(PantryApp app) {
        mApp = app;
        mDatabase = app.getDatabase();
    }

    public static PantryRepository getInstance(PantryApp app) {
        if (sInstance == null) {
            synchronized (PantryRepository.class) {
                if (sInstance == null) {
                    sInstance = new PantryRepository(app);
                }
            }
        }
        return sInstance;
    }

    // ---------------------------------------------------------------- pantry

    /** Observable stream of the pantry; Room re-emits it after every write. */
    public LiveData<List<PantryItem>> getPantryLiveData() {
        return mDatabase.pantryItemDao().getAllItems();
    }

    /** One-off snapshot for the matching engine (which runs off the main thread). */
    public void getPantrySync(Callback<List<PantryItem>> callback) {
        mApp.getExecutor().execute(() -> {
            List<PantryItem> items = mDatabase.pantryItemDao().getAllItemsSync();
            mApp.runOnUiThread(() -> callback.onResult(items));
        });
    }

    public void insertItem(PantryItem item, Runnable onComplete) {
        mApp.getExecutor().execute(() -> {
            mDatabase.pantryItemDao().insert(item);
            mApp.runOnUiThread(onComplete);
        });
    }

    public void updateItem(PantryItem item, Runnable onComplete) {
        mApp.getExecutor().execute(() -> {
            mDatabase.pantryItemDao().update(item);
            mApp.runOnUiThread(onComplete);
        });
    }

    public void deleteItem(PantryItem item, Runnable onComplete) {
        mApp.getExecutor().execute(() -> {
            mDatabase.pantryItemDao().delete(item);
            mApp.runOnUiThread(onComplete);
        });
    }

    public void deleteAllItems(Runnable onComplete) {
        mApp.getExecutor().execute(() -> {
            mDatabase.pantryItemDao().deleteAll();
            mApp.runOnUiThread(onComplete);
        });
    }

    /**
     * Loads one pantry row for the edit screen. {@code result} is {@code null} when the
     * item was deleted in the meantime (e.g. by "Delete all pantry items" in Settings).
     */
    public void getItemById(long id, Callback<PantryItem> callback) {
        mApp.getExecutor().execute(() -> {
            PantryItem item = mDatabase.pantryItemDao().getById(id);
            mApp.runOnUiThread(() -> callback.onResult(item));
        });
    }

    // --------------------------------------------------------------- recipes

    public void getAllRecipes(Callback<List<RecipeWithIngredients>> callback) {
        mApp.getExecutor().execute(() -> {
            List<RecipeWithIngredients> recipes =
                    mDatabase.recipeDao().getAllRecipesWithIngredients();
            mApp.runOnUiThread(() -> callback.onResult(recipes));
        });
    }

    /** {@code result} is {@code null} when the id is unknown or stale. */
    public void getRecipeById(long id, Callback<RecipeWithIngredients> callback) {
        mApp.getExecutor().execute(() -> {
            RecipeWithIngredients recipe = mDatabase.recipeDao().getRecipeWithIngredients(id);
            mApp.runOnUiThread(() -> callback.onResult(recipe));
        });
    }
}
