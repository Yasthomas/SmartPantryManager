package com.thomas.smartpantrymanager.logic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Unit classification and conversion for the strict-matching rule.
 *
 * Units belong to one of three families. MASS and VOLUME each have a canonical base
 * (grams and millilitres) so two compatible amounts can be added up directly. COUNT units
 * have no universal base: "2 cloves of garlic" and "2 pieces of garlic" are not obviously
 * the same amount, so COUNT units only compare when their unit keys are equal — or when
 * one side is the generic "count" (a pantry row typed as "pcs" is treated as generic).
 * Cross-family comparison (MASS vs VOLUME, COUNT vs anything else) is refused, which is
 * what makes 500 g of rice fail to satisfy a recipe asking for 500 ml of rice.
 *
 *
 * <p>Within COUNT the comparison is deliberately stricter than "they are both counts": the
 * unit keys must be <em>equal</em>. A generic pantry row (key "count", typed as pcs, piece,
 * unit, whole or left blank) therefore matches a generic recipe unit, and "clove" matches
 * "clove" — but "2 pcs garlic" never satisfies a recipe asking for "2 cloves garlic",
 * because a piece of garlic and a clove of garlic are not provably the same amount. That is
 * exactly the behaviour the acceptance tests demand (2 pcs of garlic must not unlock a recipe
 * that needs 2 cloves), and guessing otherwise would break the strict-matching rule.</p>
 *
 * <p>Why not convert mass to volume? That would need a density per ingredient
 * (500 ml of flour is not 500 g of flour) and the app has no density table, so refusing
 * the conversion is the honest, strictly-correct behaviour.</p>
 */
public final class UnitConverter {

    /** The three canonical unit families. */
    public enum UnitFamily { MASS, VOLUME, COUNT }

    /** A unit resolved to its family, its factor to the family base, and its unit key. */
    public static final class NormalizedUnit {
        public final UnitFamily family;
        /** Multiplier that converts a quantity in this unit to the family base (g / ml / pcs). */
        public final double factor;
        /** Canonical key; only meaningful for COUNT units ("count", "clove", "slice", ...). */
        public final String key;

        public NormalizedUnit(UnitFamily family, double factor, String key) {
            this.family = family;
            this.factor = factor;
            this.key = key;
        }
    }

    /** Fallback for unknown or empty unit strings: a generic count of 1 unit. */
    private static final NormalizedUnit GENERIC_COUNT =
            new NormalizedUnit(UnitFamily.COUNT, 1.0, "count");

    /** All recognised unit spellings, lower-cased, mapped to their resolved form. */
    private static final Map<String, NormalizedUnit> TABLE = new HashMap<>();

    static {
        // MASS, base = gram
        put(UnitFamily.MASS, 0.001, "mg", "mg");
        put(UnitFamily.MASS, 1.0, "g", "g");
        put(UnitFamily.MASS, 1.0, "gram", "g");
        put(UnitFamily.MASS, 1.0, "grams", "g");
        put(UnitFamily.MASS, 1000.0, "kg", "kg");
        put(UnitFamily.MASS, 1000.0, "kilogram", "kg");
        put(UnitFamily.MASS, 1000.0, "kilograms", "kg");

        // VOLUME, base = millilitre
        put(UnitFamily.VOLUME, 1.0, "ml", "ml");
        put(UnitFamily.VOLUME, 1.0, "millilitre", "ml");
        put(UnitFamily.VOLUME, 1.0, "millilitres", "ml");
        put(UnitFamily.VOLUME, 1.0, "milliliter", "ml");
        put(UnitFamily.VOLUME, 1.0, "milliliters", "ml");
        put(UnitFamily.VOLUME, 1000.0, "l", "l");
        put(UnitFamily.VOLUME, 1000.0, "litre", "l");
        put(UnitFamily.VOLUME, 1000.0, "litres", "l");
        put(UnitFamily.VOLUME, 1000.0, "liter", "l");
        put(UnitFamily.VOLUME, 1000.0, "liters", "l");
        put(UnitFamily.VOLUME, 5.0, "tsp", "tsp");
        put(UnitFamily.VOLUME, 5.0, "teaspoon", "tsp");
        put(UnitFamily.VOLUME, 5.0, "teaspoons", "tsp");
        put(UnitFamily.VOLUME, 15.0, "tbsp", "tbsp");
        put(UnitFamily.VOLUME, 15.0, "tablespoon", "tbsp");
        put(UnitFamily.VOLUME, 15.0, "tablespoons", "tbsp");
        put(UnitFamily.VOLUME, 240.0, "cup", "cup");
        put(UnitFamily.VOLUME, 240.0, "cups", "cup");

        // COUNT, base = piece. Generic spellings collapse to the shared "count" key so a
        // loosely typed pantry row ("2 pcs garlic") still matches a precise recipe unit.
        putCount("count");
        putCount("pcs");
        putCount("pc");
        putCount("piece");
        putCount("pieces");
        putCount("unit");
        putCount("units");
        putCount("whole");
        // Specific count units keep their own key: 2 cloves never silently satisfies 2 pcs.
        putCount("clove");
        putCount("cloves");
        putCount("slice");
        putCount("slices");
        putCount("can");
        putCount("cans");
        putCount("bunch");
        putCount("bunches");
        putCount("pinch");
        putCount("pinches");
    }

    private static void put(UnitFamily family, double factor, String alias, String key) {
        TABLE.put(alias, new NormalizedUnit(family, factor, key));
    }

    private static void putCount(String alias) {
        // Singularise so "cloves" and "clove" share the key "clove"; generic spellings get "count".
        String key = isGenericCount(alias) ? "count" : singularizeUnit(alias);
        TABLE.put(alias, new NormalizedUnit(UnitFamily.COUNT, 1.0, key));
    }

    private static boolean isGenericCount(String alias) {
        return alias.isEmpty() || alias.equals("pcs") || alias.equals("pc")
                || alias.equals("piece") || alias.equals("unit") || alias.equals("whole")
                || alias.equals("count");
    }

    private static String singularizeUnit(String unit) {
        if (unit.endsWith("ches") || unit.endsWith("shes") || unit.endsWith("ses")
                || unit.endsWith("xes") || unit.endsWith("zes")) {
            return unit.substring(0, unit.length() - 2);
        }
        if (unit.endsWith("s") && !unit.endsWith("ss") && unit.length() > 2) {
            return unit.substring(0, unit.length() - 1);
        }
        return unit;
    }

    private UnitConverter() {
    }

    /**
     * Resolves a raw unit string. Case-insensitive, trimmed; an empty or completely unknown
     * unit degrades to a generic COUNT rather than throwing, because a user typing "splash"
     * must not crash the matcher — it simply has to be compared strictly.
     */
    public static NormalizedUnit normalize(String unit) {
        if (unit == null) {
            return GENERIC_COUNT;
        }
        NormalizedUnit found = TABLE.get(unit.trim().toLowerCase(Locale.ROOT));
        return found != null ? found : GENERIC_COUNT;
    }

    /**
     * Two units are comparable only inside the same family. MASS and VOLUME convert through
     * their base unit, so any two of them are compatible; COUNT units are compatible only
     * when their keys are identical (generic "count" equals generic "count", "clove" equals
     * "clove", and "clove" never equals "pcs"). Cross-family pairs are always incompatible,
     * which is what makes 500 g of rice fail to satisfy 500 ml of rice.
     */
    public static boolean isCompatible(NormalizedUnit a, NormalizedUnit b) {
        if (a == null || b == null || a.family != b.family) {
            return false;
        }
        if (a.family == UnitFamily.COUNT) {
            return a.key.equals(b.key);
        }
        return true; // MASS <-> MASS and VOLUME <-> VOLUME convert through the base unit
    }

    /** Converts a quantity to its family base (grams / millilitres / pieces). */
    public static double toBase(double qty, NormalizedUnit unit) {
        return qty * unit.factor;
    }

    /**
     * True when {@code unit} is one of the spellings in this class's table.
     *
     * The add/edit form calls this so a typo ("grm", "gr") is rejected instead of quietly
     * becoming a generic count: guessing a family would change what the strict matcher accepts.
     */
    public static boolean isRecognised(String unit) {
        if (unit == null) {
            return false;
        }
        return TABLE.containsKey(unit.trim().toLowerCase(Locale.ROOT));
    }
}
