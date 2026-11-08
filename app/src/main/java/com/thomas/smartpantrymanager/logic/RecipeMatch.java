package com.thomas.smartpantrymanager.logic;

import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;

import java.util.Collections;
import java.util.List;

/**
 * The result of running the strict-matching rule against one recipe.
 *
 * Immutable by design: the matching engine produces these on the background thread and the
 * UI only ever reads them afterwards, so nothing can mutate a result mid-render.
 */
public class RecipeMatch {

    /** The recipe this result belongs to. */
    public final RecipeWithIngredients recipe;

    /** True only when every single ingredient is available in sufficient quantity. */
    public final boolean canMake;

    /**
     * The ingredients that failed the check (absent, too small, or in an incompatible unit).
     * Empty exactly when {@link #canMake} is true. A recipe missing one ingredient ends up
     * here, which is what the "Almost There" section lists.
     */
    public final List<RecipeIngredient> missing;

    public RecipeMatch(RecipeWithIngredients recipe, boolean canMake,
                       List<RecipeIngredient> missing) {
        this.recipe = recipe;
        this.canMake = canMake;
        this.missing = missing == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(missing);
    }
}
