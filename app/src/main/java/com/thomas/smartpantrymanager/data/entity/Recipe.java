package com.thomas.smartpantrymanager.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A recipe header. Its ingredients live in {@link RecipeIngredient} rows keyed by
 * {@link #id}, which is what {@code RecipeWithIngredients} joins back together.
 */
@Entity(tableName = "recipes")
public class Recipe {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    /** Preparation steps, newline separated (one step per line). */
    public String steps;

    /** True for the recipes shipped with the app; the user cannot edit or delete these. */
    public boolean isSeed;

    public Recipe() {
    }
}
