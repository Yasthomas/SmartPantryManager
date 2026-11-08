package com.thomas.smartpantrymanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thomas.smartpantrymanager.PantryApp;
import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.PantryRepository;
import com.thomas.smartpantrymanager.data.entity.PantryItem;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;
import com.thomas.smartpantrymanager.logic.MatchingEngine;
import com.thomas.smartpantrymanager.logic.RecipeMatch;
import com.thomas.smartpantrymanager.ui.adapter.RecipeAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 3 — Suggested Recipes: the strict-matching rule made visible.
 *
 * Recomputed in {@link #onResume()} (not cached) so returning from an edit always shows
 * the truth: lowering a quantity below the requirement makes a recipe vanish, adding the
 * missing ingredient makes it appear.
 *
 * Two lists are shown and they are never merged: the strict list only ever contains
 * recipes whose ingredients are all available, while the separate "Almost There" list
 * holds recipes short by exactly one ingredient — useful, but clearly labelled as not
 * yet cookable.
 */
public class SuggestedRecipesActivity extends AppCompatActivity
        implements RecipeAdapter.OnRecipeClickListener {

    private PantryRepository mRepository;

    private RecipeAdapter mStrictAdapter;
    private RecipeAdapter mAlmostAdapter;
    private View mEmptyState;
    private View mAlmostHeader;
    private View mAlmostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        mRepository = PantryRepository.getInstance((PantryApp) getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_suggested);

        mEmptyState = findViewById(R.id.empty_state);
        mAlmostHeader = findViewById(R.id.almost_header);
        RecyclerView almostList = findViewById(R.id.almost_list);
        mAlmostList = almostList;

        RecyclerView strictList = findViewById(R.id.suggestion_list);
        strictList.setLayoutManager(new LinearLayoutManager(this));
        mStrictAdapter = new RecipeAdapter(this, false);
        strictList.setAdapter(mStrictAdapter);

        almostList.setLayoutManager(new LinearLayoutManager(this));
        mAlmostAdapter = new RecipeAdapter(this, true);
        almostList.setAdapter(mAlmostAdapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        setupBottomNav(bottomNav, R.id.nav_suggestions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMatches();
    }

    /**
     * Loads the pantry and the recipes on the background executor, runs the matching
     * engine there, then posts the two separated result lists back to the main thread.
     * Nested callbacks are used instead of a latch because both reads are already queued
     * on the same single-thread executor in order.
     */
    private void loadMatches() {
        mRepository.getPantrySync(pantry -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            mRepository.getAllRecipes(recipes -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showResults(MatchingEngine.matchAll(recipes, pantry));
            });
        });
    }

    private void showResults(List<RecipeMatch> matches) {
        List<RecipeWithIngredients> cookable = new ArrayList<>();
        List<RecipeWithIngredients> almost = new ArrayList<>();
        Map<Long, String> missingLabels = new HashMap<>();

        for (RecipeMatch match : matches) {
            if (match.recipe == null || match.recipe.recipe == null) {
                continue;
            }
            if (match.canMake) {
                cookable.add(match.recipe);
            } else if (match.missing.size() == 1) {
                // Exactly one ingredient short: interesting, but strictly not suggested.
                almost.add(match.recipe);
                missingLabels.put(match.recipe.recipe.id, match.missing.get(0).name);
            }
        }

        mStrictAdapter.submitList(cookable);
        mAlmostAdapter.setMissingLabels(missingLabels);
        mAlmostAdapter.submitList(almost);

        mEmptyState.setVisibility(cookable.isEmpty() ? View.VISIBLE : View.GONE);
        boolean showAlmost = !almost.isEmpty();
        mAlmostHeader.setVisibility(showAlmost ? View.VISIBLE : View.GONE);
        mAlmostList.setVisibility(showAlmost ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRecipeClick(RecipeWithIngredients recipe) {
        if (recipe == null || recipe.recipe == null) {
            return;
        }
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.recipe.id);
        startActivity(intent);
    }

    // -------------------------------------------------------------- navigation

    /** Selected item first, listener second — see MainActivity.setupBottomNav. */
    private void setupBottomNav(BottomNavigationView bottomNav, int selectedItemId) {
        bottomNav.setSelectedItemId(selectedItemId);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) {
                return true;
            }
            Class<?> target;
            if (id == R.id.nav_pantry) {
                target = MainActivity.class;
            } else if (id == R.id.nav_suggestions) {
                target = SuggestedRecipesActivity.class;
            } else if (id == R.id.nav_settings) {
                target = SettingsActivity.class;
            } else {
                return false;
            }
            openTopLevel(target);
            return true;
        });
    }

    private void openTopLevel(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
