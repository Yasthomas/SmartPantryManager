package com.thomas.smartpantrymanager.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One ingredient currently in the user's pantry.
 *
 * {@code name} is stored exactly as the user typed it; normalisation happens at match time
 * (see {@code IngredientNormalizer}) so the UI always shows what the user actually wrote.
 */
@Entity(tableName = "pantry_items")
public class PantryItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Ingredient name as typed by the user. */
    public String name;

    /** Amount the user has, interpreted in {@link #unit}. */
    public double quantity;

    /** Unit string, e.g. "g", "ml", "pcs", "cloves". */
    public String unit;

    /** Epoch millis of the expiry date, or {@code null} when the user did not set one. */
    public Long expiryDate;

    /** Epoch millis of creation; gives the list a stable tie-breaker when names repeat. */
    public long createdAt;

    public PantryItem() {
        this.createdAt = System.currentTimeMillis();
    }
}
