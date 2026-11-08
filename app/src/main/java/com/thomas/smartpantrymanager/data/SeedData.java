package com.thomas.smartpantrymanager.data;

import com.thomas.smartpantrymanager.data.dao.RecipeDao;
import com.thomas.smartpantrymanager.data.entity.Recipe;
import com.thomas.smartpantrymanager.data.entity.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * The eighteen recipes the app ships with.
 *
 * The ingredient lists are deliberately small (3-6 rows) and use everyday quantities so
 * that a strict match is achievable by hand during a demo — that is the whole point of
 * the strict-matching rule. Singular and plural names are mixed on purpose ("tomatoes"
 * in one recipe, "tomato" in another) so {@code IngredientNormalizer} is exercised by
 * the seeded data itself, and the unit strings come from the canonical unit list.
 */
public final class SeedData {

    private SeedData() {
    }

    /**
     * Inserts every recipe with its ingredients. Must only be called when
     * {@code recipeDao().countRecipes() == 0} and always on the background executor;
     * {@code PantryApp} guarantees both.
     */
    public static void seed(RecipeDao dao) {
        insert(dao, "Garlic Butter Pasta",
                "pasta|200|g; butter|30|g; garlic|2|cloves",
                "Boil the pasta in salted water until al dente.\n"
                        + "Melt the butter and fry the minced garlic for 30 seconds.\n"
                        + "Drain the pasta and toss it through the garlic butter.\n"
                        + "Serve immediately.");

        insert(dao, "Tomato Toast",
                "bread|2|slices; tomatoes|1|pcs; olive oil|1|tbsp; salt|1|pinch",
                "Toast the bread slices until golden.\n"
                        + "Dice the tomato and pile it onto the toast.\n"
                        + "Drizzle with olive oil and finish with a pinch of salt.");

        insert(dao, "Veggie Omelette",
                "eggs|3|pcs; onion|1|pcs; tomato|1|pcs; oil|1|tbsp",
                "Beat the eggs with a pinch of salt.\n"
                        + "Sauté the chopped onion and tomato in the oil.\n"
                        + "Pour in the eggs and cook until just set.\n"
                        + "Fold and slide onto a plate.");

        insert(dao, "Cheesy Scrambled Eggs",
                "eggs|2|pcs; butter|10|g; cheese|30|g",
                "Melt the butter in a pan over low heat.\n"
                        + "Add the beaten eggs and stir gently and constantly.\n"
                        + "Fold in the grated cheese until melted and glossy.");

        insert(dao, "Fried Rice",
                "rice|300|g; egg|1|pcs; onion|1|pcs; soy sauce|1|tbsp; oil|1|tbsp",
                "Heat the oil and fry the chopped onion until soft.\n"
                        + "Add the cold cooked rice and stir fry for 3 minutes.\n"
                        + "Push the rice aside, scramble the egg, then mix it through.\n"
                        + "Stir in the soy sauce and serve.");

        insert(dao, "Potato Wedges",
                "potatoes|500|g; oil|2|tbsp; salt|1|pinch",
                "Preheat the oven to 200 degrees.\n"
                        + "Cut the potatoes into wedges and toss with oil and salt.\n"
                        + "Bake for 30 minutes, turning once halfway.");

        insert(dao, "Creamy Tomato Soup",
                "tomatoes|500|g; onion|1|pcs; cream|100|ml; garlic|1|clove",
                "Simmer the chopped onion and garlic until soft.\n"
                        + "Add the chopped tomatoes and cook for 15 minutes.\n"
                        + "Blend until smooth, then stir in the cream.\n"
                        + "Season and serve warm.");

        insert(dao, "Banana Oat Pancakes",
                "banana|1|pcs; oats|100|g; egg|1|pcs; milk|100|ml",
                "Blend the banana, oats, egg and milk into a smooth batter.\n"
                        + "Heat a lightly oiled pan over medium heat.\n"
                        + "Cook small pancakes for about 2 minutes per side.");

        insert(dao, "Peanut Butter Banana Toast",
                "bread|1|slices; peanut butter|2|tbsp; banana|1|pcs",
                "Toast the bread until crisp.\n"
                        + "Spread generously with peanut butter.\n"
                        + "Top with sliced banana and serve.");

        insert(dao, "Simple Green Salad",
                "lettuce|100|g; cucumber|1|pcs; olive oil|1|tbsp; lemon juice|1|tbsp",
                "Wash and tear the lettuce into bite-sized pieces.\n"
                        + "Slice the cucumber and toss it with the lettuce.\n"
                        + "Dress with olive oil and lemon juice.");

        insert(dao, "Cheese Quesadilla",
                "tortilla|1|pcs; cheese|50|g; butter|10|g",
                "Butter one side of the tortilla.\n"
                        + "Scatter the cheese over the other side and fold in half.\n"
                        + "Fry until golden and crisp on both sides.");

        insert(dao, "Tuna Mayo Sandwich",
                "bread|2|slices; tuna|1|can; mayonnaise|2|tbsp",
                "Drain the tuna and mix it with the mayonnaise.\n"
                        + "Spread onto one slice of bread.\n"
                        + "Top with the second slice and cut in half.");

        insert(dao, "Garlic Noodles",
                "noodles|200|g; garlic|2|cloves; soy sauce|1|tbsp; butter|10|g",
                "Boil the noodles and drain them well.\n"
                        + "Melt the butter and fry the sliced garlic until fragrant.\n"
                        + "Toss the noodles with the soy sauce and garlic butter.");

        insert(dao, "Vegetable Stir Fry",
                "broccoli|200|g; carrot|1|pcs; soy sauce|1|tbsp; oil|1|tbsp",
                "Cut the broccoli and carrot into bite-sized pieces.\n"
                        + "Heat the oil in a wok over high heat.\n"
                        + "Stir fry the vegetables for 4 minutes.\n"
                        + "Add the soy sauce and toss to coat.");

        insert(dao, "Oatmeal with Milk",
                "oats|50|g; milk|200|ml; sugar|1|tbsp",
                "Bring the milk to a gentle simmer.\n"
                        + "Stir in the oats and cook for 5 minutes.\n"
                        + "Add the sugar, stir once more and serve.");

        insert(dao, "French Toast",
                "bread|2|slices; egg|1|pcs; milk|50|ml; butter|10|g",
                "Whisk the egg with the milk.\n"
                        + "Dip the bread slices into the mixture until soaked.\n"
                        + "Fry in the butter until golden on both sides.");

        insert(dao, "Chickpea Salad",
                "chickpeas|1|can; tomatoes|1|pcs; cucumber|1|pcs; olive oil|1|tbsp",
                "Drain and rinse the chickpeas.\n"
                        + "Dice the tomato and cucumber.\n"
                        + "Toss everything with olive oil and a pinch of salt.");

        insert(dao, "Pasta with Tomato Sauce",
                "pasta|200|g; tomatoes|300|g; garlic|1|clove; olive oil|1|tbsp",
                "Boil the pasta until al dente.\n"
                        + "Fry the garlic in olive oil, then add the chopped tomatoes.\n"
                        + "Simmer for 10 minutes and toss through the pasta.");
    }

    /**
     * Inserts one recipe and its ingredient rows.
     *
     * The ingredient spec uses a compact "name|qty|unit; name|qty|unit" notation to keep
     * eighteen recipes readable in one file; it is parsed here once, at seed time, so no
     * malformed string can ever reach the database.
     */
    private static void insert(RecipeDao dao, String name, String ingredientSpec, String steps) {
        Recipe recipe = new Recipe();
        recipe.name = name;
        recipe.steps = steps;
        recipe.isSeed = true;
        long recipeId = dao.insertRecipe(recipe);

        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (String entry : ingredientSpec.split(";")) {
            String[] parts = entry.trim().split("\\|");
            if (parts.length != 3) {
                continue; // defensive: never persist a half-parsed ingredient
            }
            double quantity;
            try {
                quantity = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            ingredients.add(new RecipeIngredient(
                    recipeId,
                    parts[0].trim(),
                    quantity,
                    parts[2].trim()
            ));
        }
        dao.insertRecipeIngredients(ingredients);
    }
}
