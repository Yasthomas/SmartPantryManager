package com.thomas.smartpantrymanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One required ingredient of a recipe.
 *
 * The foreign key cascades on delete so removing a recipe can never leave orphan rows
 * behind, and the index on {@code recipeId} keeps the {@code @Relation} join fast.
 */
@Entity(
        tableName = "recipe_ingredients",
        foreignKeys = @ForeignKey(
                entity = Recipe.class,
                parentColumns = "id",
                childColumns = "recipeId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("recipeId")}
)
public class RecipeIngredient {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long recipeId;

    public String name;

    public double quantity;

    public String unit;

    public RecipeIngredient() {
    }

    // Not the constructor Room instantiates; @Ignore keeps the compiler warning away.
    @Ignore
    public RecipeIngredient(long recipeId, String name, double quantity, String unit) {
        this.recipeId = recipeId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
}
