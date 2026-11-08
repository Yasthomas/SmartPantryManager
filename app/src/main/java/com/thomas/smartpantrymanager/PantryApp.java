package com.thomas.smartpantrymanager;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.thomas.smartpantrymanager.data.AppDatabase;
import com.thomas.smartpantrymanager.data.SeedData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application-wide singleton.
 *
 * Holds the single {@link AppDatabase} instance and the single-thread {@link ExecutorService}
 * that every Room call in the app runs on. Keeping both here (instead of in an Activity)
 * means database work survives configuration changes and never leaks an Activity context.
 */
public class PantryApp extends Application {

    private AppDatabase mDatabase;
    private ExecutorService mExecutor;
    private Handler mMainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabase = AppDatabase.getInstance(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mMainHandler = new Handler(Looper.getMainLooper());
        seedIfNeeded();
    }

    /**
     * Seeds the reference recipes on first launch only. The check runs on the background
     * executor so the main thread never touches SQLite; countRecipes() == 0 is the
     * "database was just created" signal, so seeding happens exactly once even if the
     * process is killed and restarted later.
     */
    private void seedIfNeeded() {
        mExecutor.execute(() -> {
            if (mDatabase.recipeDao().countRecipes() == 0) {
                // One transaction: a crash halfway through must not leave a partially seeded
                // database behind, because countRecipes() > 0 would then stop every retry.
                mDatabase.runInTransaction(() -> SeedData.seed(mDatabase.recipeDao()));
            }
        });
    }

    public AppDatabase getDatabase() {
        return mDatabase;
    }

    public ExecutorService getExecutor() {
        return mExecutor;
    }

    /** Posts a result coming off the executor back onto the main thread. */
    public void runOnUiThread(Runnable runnable) {
        mMainHandler.post(runnable);
    }
}
