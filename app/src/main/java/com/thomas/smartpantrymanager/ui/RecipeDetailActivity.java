package com.thomas.smartpantrymanager.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.thomas.smartpantrymanager.PantryApp;
import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.PantryRepository;
import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;
import com.google.android.material.appbar.MaterialToolbar;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Screen 4 — Recipe Detail: everything needed to actually cook the dish.
 *
 * Receives the recipe id through {@link #EXTRA_RECIPE_ID}, loads the recipe with its
 * ingredients on the background executor, and renders "quantity unit name" rows plus the
 * numbered preparation steps. A missing or stale id (recipe gone) exits cleanly instead
 * of crashing.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    /** Extra carrying the recipe id to display. */
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";
    private static final long INVALID_ID = -1L;

    private PantryRepository mRepository;

    private TextView mIngredientCount;
    private LinearLayout mIngredientRows;
    private TextView mSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        mRepository = PantryRepository.getInstance((PantryApp) getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        mIngredientCount = findViewById(R.id.ingredient_count);
        mIngredientRows = findViewById(R.id.ingredient_rows);
        mSteps = findViewById(R.id.preparation_steps);

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, INVALID_ID);
        if (recipeId == INVALID_ID) {
            Toast.makeText(this, R.string.toast_recipe_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRecipe(recipeId);
    }

    private void loadRecipe(long recipeId) {
        mRepository.getRecipeById(recipeId, recipe -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (recipe == null || recipe.recipe == null) {
                Toast.makeText(RecipeDetailActivity.this,
                        R.string.toast_recipe_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            bind(recipe);
        });
    }

    private void bind(RecipeWithIngredients recipe) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(recipe.recipe.name);

        mIngredientCount.setText(getResources().getQuantityString(
                R.plurals.ingredient_count,
                recipe.ingredients == null ? 0 : recipe.ingredients.size(),
                recipe.ingredients == null ? 0 : recipe.ingredients.size()));

        mIngredientRows.removeAllViews();
        if (recipe.ingredients != null) {
            for (RecipeIngredient ingredient : recipe.ingredients) {
                TextView row = new TextView(this);
                row.setText(getString(R.string.ingredient_row,
                        formatQuantity(ingredient.quantity),
                        ingredient.unit == null ? "" : ingredient.unit,
                        ingredient.name == null ? "" : ingredient.name));
                row.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.detail_row_text_size));
                int rowPadding = getResources().getDimensionPixelSize(
                        R.dimen.detail_row_vertical_padding);
                row.setPadding(0, rowPadding, 0, rowPadding);
                mIngredientRows.addView(row);
            }
        }

        mSteps.setText(formatSteps(recipe.recipe.steps));
    }

    /**
     * Numbers the newline-separated steps ("Boil water." -> "1. Boil water.") so the user
     * can follow them at a glance while keeping every line break from the stored string.
     */
    private String formatSteps(String steps) {
        if (steps == null || steps.trim().isEmpty()) {
            return getString(R.string.no_steps);
        }
        String[] lines = steps.split("\n");
        StringBuilder builder = new StringBuilder();
        int number = 1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            builder.append(getString(R.string.step_row, number++, trimmed)).append('\n');
        }
        return builder.toString().trim();
    }

    /** No scientific notation, no trailing ".0" — same rule used by both list adapters. */
    private static String formatQuantity(double quantity) {
        if (Double.isNaN(quantity) || Double.isInfinite(quantity)) {
            return "0";
        }
        long whole = (long) quantity;
        if (quantity == whole) {
            return Long.toString(whole);
        }
        return BigDecimal.valueOf(quantity)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
