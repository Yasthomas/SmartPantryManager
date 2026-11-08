package com.thomas.smartpantrymanager.data.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.thomas.smartpantrymanager.data.entity.Recipe;
import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;

import java.util.List;

/**
 * A recipe together with all of its ingredient rows.
 *
 * Room fills {@link #ingredients} by joining recipe_ingredients on recipeId; queried
 * through a DAO method annotated with {@code @Transaction} so the header and its rows
 * are always read as one consistent snapshot.
 */
public class RecipeWithIngredients {

    @Embedded
    public Recipe recipe;

    @Relation(parentColumn = "id", entityColumn = "recipeId")
    public List<RecipeIngredient> ingredients;
}
