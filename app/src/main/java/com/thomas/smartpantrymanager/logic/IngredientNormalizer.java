package com.thomas.smartpantrymanager.logic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns free-form ingredient names into a canonical matching key.
 *
 * Both the pantry rows and the recipe rows are normalised at match time — stored names are
 * never mutated, so the UI keeps showing exactly what the user typed ("Tomatoes", "tomatoes",
 * "Tomato" all display as entered but all match the same recipe).
 *
 * The pipeline is: trim + lower-case (Locale.ROOT, so Turkish devices do not turn "I" into
 * a dotless ı) → replace every non-alphanumeric character with a space → collapse whitespace →
 * singularise the last word → apply the synonym alias map.
 */
public final class IngredientNormalizer {

    /** Synonyms mapped after singularisation, all lower case. */
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("capsicum", "bell pepper");
        ALIASES.put("aubergine", "eggplant");
        ALIASES.put("coriander", "cilantro");
        ALIASES.put("spring onion", "scallion");
        // Chickpea is both singular and plural-looking; the entry documents the canonical
        // form so "chickpeas" (normalised to "chickpea") always lands on the same key.
        ALIASES.put("chickpea", "chickpea");
    }

    /**
     * Irregular plurals: the -ves and -ren forms that no suffix rule can derive, plus the
     * words whose plural is a different word entirely.
     */
    private static final Map<String, String> IRREGULAR_PLURALS = new HashMap<>();

    static {
        IRREGULAR_PLURALS.put("children", "child");
        IRREGULAR_PLURALS.put("loaves", "loaf");
        IRREGULAR_PLURALS.put("leaves", "leaf");
        IRREGULAR_PLURALS.put("knives", "knife");
    }

    private IngredientNormalizer() {
    }

    /**
     * @param raw name as stored by the user or shipped in the seed data; may be null
     * @return a stable, lower-case, singular, alias-resolved key ("" for null/blank input)
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            return "";
        }
        // Any character that is not a letter or a digit becomes a separator, then runs of
        // separators collapse to a single space: "olive-oil!!" and "olive  oil" both -> "olive oil".
        cleaned = cleaned.replaceAll("[^a-z0-9]+", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        int lastSpace = cleaned.lastIndexOf(' ');
        String lastWord = lastSpace == -1 ? cleaned : cleaned.substring(lastSpace + 1);
        String head = lastSpace == -1 ? "" : cleaned.substring(0, lastSpace + 1);
        String normalized = head + singularize(lastWord);

        String alias = ALIASES.get(normalized);
        return alias != null ? alias : normalized;
    }

    /**
     * Singularises only the final word of a phrase, because only the head noun varies:
     * "spring onions" -> "spring onion", "olive oils" is never needed.
     *
     * Ordered rules (first match wins): irregular map (children, loaves, leaves, knives),
     * ies -> y, oes -> drop "es", ses/xes/zes/ches/shes -> drop "es", plain trailing s -> drop
     * "s". The plain-s rule is guarded so it never truncates a word ending in a double s
     * ("glass", "dress") or a word that only looks plural because it ends in "us"/"is" —
     * "asparagus" and "basis" are already singular and must stay intact.
     */
    static String singularize(String word) {
        String irregular = IRREGULAR_PLURALS.get(word);
        if (irregular != null) {
            return irregular;
        }
        if (word.length() > 3 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";      // berries -> berry
        }
        if (word.endsWith("oes")) {
            return word.substring(0, word.length() - 2);            // tomatoes -> tomato
        }
        if (word.endsWith("ses") || word.endsWith("xes") || word.endsWith("zes")
                || word.endsWith("ches") || word.endsWith("shes")) {
            return word.substring(0, word.length() - 2);            // boxes -> box, pulses -> puls?
        }
        if (word.endsWith("s") && !word.endsWith("ss")
                && !word.endsWith("us") && !word.endsWith("is") && word.length() > 2) {
            return word.substring(0, word.length() - 1);            // eggs -> egg, tomatoes handled above
        }
        return word;
    }
}
