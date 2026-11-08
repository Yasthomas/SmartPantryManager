package com.thomas.smartpantrymanager.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.thomas.smartpantrymanager.data.entity.Recipe;
import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;

import java.util.List;

/**
 * Reads recipes joined with their ingredients, plus the insert helpers used for seeding.
 *
 * Every read returns {@link RecipeWithIngredients} rather than a bare {@link Recipe}:
 * the matching engine always needs the ingredient rows, and loading them in one
 * transactional query avoids the classic "header loaded now, rows loaded later" race.
 */
@Dao
public interface RecipeDao {

    /**
     * @Transaction is required for @Relation: it wraps the header query and the per-recipe
     * ingredient queries in a single transaction so a seed insert happening concurrently
     * can never produce a recipe with half of its ingredients.
     */
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC")
    List<RecipeWithIngredients> getAllRecipesWithIngredients();

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    RecipeWithIngredients getRecipeWithIngredients(long id);

    @Insert
    long insertRecipe(Recipe recipe);

    @Insert
    void insertRecipeIngredients(List<RecipeIngredient> ingredients);

    /** Cheap "has anything been seeded yet?" check, run on the background executor. */
    @Query("SELECT COUNT(*) FROM recipes")
    int countRecipes();
}
