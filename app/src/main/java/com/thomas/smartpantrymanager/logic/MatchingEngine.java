package com.thomas.smartpantrymanager.logic;

import com.thomas.smartpantrymanager.data.entity.PantryItem;
import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implements the app's core rule: a recipe is suggested only when the pantry holds
 * <b>every</b> ingredient it needs, in at least the required quantity.
 *
 * All matching happens on the background executor — this class does pure computation over
 * plain lists and never touches the database or the UI.
 */
public final class MatchingEngine {

    /**
     * Tolerance for floating point comparison. Summing 0.1 + 0.2 in binary floating point
     * gives 0.30000000000000004, so without a nudge a pantry holding exactly 0.3 would be
     * told it does not have enough.
     */
    private static final double EPSILON = 1e-6;

    private MatchingEngine() {
    }

    /**
     * Evaluates one recipe against the whole pantry.
     *
     * Algorithm:
     * <ol>
     *   <li>index the pantry by normalised ingredient name (rows are never mutated),</li>
     *   <li>for each required ingredient, normalise its name and look up the pantry rows,</li>
     *   <li>convert every <em>unit-compatible</em> row to the family base unit and sum the
     *       amounts; incompatible rows are skipped rather than guessed at,</li>
     *   <li>the ingredient is satisfied when {@code sum + 1e-6 >= requiredBase},</li>
     *   <li>{@code canMake} is true only if no ingredient ended up unsatisfied; everything
     *       unsatisfied is collected into {@code missing} (used by "Almost There").</li>
     * </ol>
     *
     * @param recipe the recipe to check, with its ingredients already loaded
     * @param pantry every row currently in the user's pantry
     * @return an immutable match carrying the verdict and the missing ingredients
     */
    public static RecipeMatch evaluate(RecipeWithIngredients recipe, List<PantryItem> pantry) {
        if (recipe == null || recipe.recipe == null) {
            return new RecipeMatch(recipe, false, Collections.emptyList());
        }

        Map<String, List<PantryItem>> pantryByName = indexPantry(pantry);
        List<RecipeIngredient> missing = new ArrayList<>();

        List<RecipeIngredient> ingredients =
                recipe.ingredients == null ? Collections.emptyList() : recipe.ingredients;

        for (RecipeIngredient ingredient : ingredients) {
            String key = IngredientNormalizer.normalize(ingredient.name);
            List<PantryItem> rows = pantryByName.get(key);

            if (rows == null || rows.isEmpty()) {
                missing.add(ingredient); // not in the pantry at all
                continue;
            }

            UnitConverter.NormalizedUnit requiredUnit = UnitConverter.normalize(ingredient.unit);
            double requiredBase = UnitConverter.toBase(ingredient.quantity, requiredUnit);

            double availableBase = 0.0;
            for (PantryItem row : rows) {
                UnitConverter.NormalizedUnit pantryUnit = UnitConverter.normalize(row.unit);
                if (UnitConverter.isCompatible(requiredUnit, pantryUnit)) {
                    availableBase += UnitConverter.toBase(row.quantity, pantryUnit);
                }
                // incompatible units are skipped: guessing between g and ml is not allowed
            }

            if (availableBase + EPSILON < requiredBase) {
                missing.add(ingredient); // present but not enough, or only in incompatible units
            }
        }

        return new RecipeMatch(recipe, missing.isEmpty(), missing);
    }

    /**
     * Evaluates every recipe and returns one {@link RecipeMatch} per recipe, sorted
     * alphabetically by recipe name so the suggestions screen has a stable order.
     * The caller filters on {@link RecipeMatch#canMake} for the strict list.
     */
    public static List<RecipeMatch> matchAll(List<RecipeWithIngredients> recipes,
                                             List<PantryItem> pantry) {
        List<RecipeMatch> results = new ArrayList<>();
        if (recipes == null) {
            return results;
        }
        for (RecipeWithIngredients recipe : recipes) {
            results.add(evaluate(recipe, pantry == null ? Collections.emptyList() : pantry));
        }
        Collections.sort(results, Comparator.comparing(
                match -> match.recipe == null || match.recipe.recipe == null
                        ? ""
                        : match.recipe.recipe.name.toLowerCase(Locale.ROOT)));
        return results;
    }

    /** Groups pantry rows under their normalised name; identical names merge quantities. */
    private static Map<String, List<PantryItem>> indexPantry(List<PantryItem> pantry) {
        Map<String, List<PantryItem>> index = new HashMap<>();
        if (pantry == null) {
            return index;
        }
        for (PantryItem item : pantry) {
            if (item == null) {
                continue;
            }
            String key = IngredientNormalizer.normalize(item.name);
            if (key.isEmpty()) {
                continue;
            }
            List<PantryItem> rows = index.get(key);
            if (rows == null) {
                rows = new ArrayList<>();
                index.put(key, rows);
            }
            rows.add(item);
        }
        return index;
    }
}
