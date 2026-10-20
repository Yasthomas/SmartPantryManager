# Navigation Architecture — Design Decisions

## Summary

The Smart Pantry Manager uses a **hybrid navigation model**:

- A **BottomNavigationView** hosts the three primary top-level destinations (Pantry, Suggestions, Settings) as **Fragments** inside a single host Activity (`MainActivity`).
- **Add/Edit Ingredient** and **Recipe Detail** are launched as **separate Activities** on top of the host, because they are modal, task-focused screens rather than top-level destinations.

## Why BottomNavigationView (and not a Navigation Drawer or Toolbar Menu)?

| Option | Verdict | Reason |
|---|---|---|
| **BottomNavigationView** | **Chosen** | Ideal for 3–5 top-level destinations. On modern phones it sits in the thumb zone — easy to reach one-handed. Google's Material Design guidelines explicitly recommend it for apps with a small number of equally-important top-level screens. |
| Navigation Drawer | Rejected | Overkill for three destinations. Drawers are better suited to apps with 5+ top-level sections and less frequent switching between them. |
| Toolbar menu (overflow) | Rejected | Hides navigation behind a tap. Poor discoverability — the user has to guess what's behind the hamburger icon. |
| Tabs (TabLayout) | Rejected | Visually too similar to BottomNavigationView for a small app and less ergonomic (top of the screen is harder to reach one-handed). |

## Why Fragments (not Activities) for the Three Tabs?

The three top-level screens — Pantry, Suggestions, Settings — share:

- The same `MainActivity` shell (toolbar, bottom nav bar)
- The same `ViewModel` scope for sharing pantry and recipe data
- The same lifecycle state during tab switching

Using Fragments means the user can switch tabs without rebuilding the Activity, and any shared state (e.g. the pantry list, the matching results) is preserved naturally. If we used separate Activities for each tab, we'd either duplicate the bottom nav bar in each one or have to spawn new Activities each time — both wrong for a peer-level destination model.

## Why Activities (not Fragments) for Add/Edit Ingredient and Recipe Detail?

Both screens:

- Are **modal** — the user is completing a specific task and will return once done
- Should participate in the **back stack** — pressing Back should dismiss them and return to the previous screen
- Do **not** appear in the bottom navigation bar
- Should **not** preserve state across tab switches (closing them discards any unsaved edits)

Activities are the natural fit: Android's back stack handles them automatically, and their lifecycle is cleanly decoupled from the bottom-nav Fragments.

## Screen-to-Screen Transitions

| From | To | Trigger | Mechanism |
|---|---|---|---|
| App launch | Pantry List | Launcher icon | `MainActivity` opens, defaults to Pantry tab |
| Pantry List | Add/Edit Ingredient (create) | FAB (+) tap | `Intent` to `AddEditIngredientActivity` with no extras |
| Pantry List | Add/Edit Ingredient (edit) | List item tap | `Intent` to `AddEditIngredientActivity` with `EXTRA_INGREDIENT_ID` |
| Add/Edit Ingredient | Pantry List | Save or Back | `finish()` returns to host; `LiveData` auto-refreshes the list |
| Pantry List | Suggested Recipes | Bottom nav tab 2 | `FragmentTransaction` (via `BottomNavigationView.setOnItemSelectedListener`) |
| Suggested Recipes | Recipe Detail | List item tap | `Intent` to `RecipeDetailActivity` with `EXTRA_RECIPE_ID` |
| Recipe Detail | Suggested Recipes | Back | `finish()` returns to host |
| Any tab | Settings | Bottom nav tab 3 | `FragmentTransaction` |

## Passing Data Between Screens

- **Pantry List → Add/Edit (edit mode):** `Intent.putExtra("EXTRA_INGREDIENT_ID", id)`
- **Suggested Recipes → Recipe Detail:** `Intent.putExtra("EXTRA_RECIPE_ID", id)`
- **Add/Edit → Pantry List:** No result is passed back; instead, both screens observe the same `LiveData<List<PantryItem>>` from the shared `PantryViewModel`, so any `insert`/`update`/`delete` automatically propagates.

The LiveData approach avoids the deprecated `startActivityForResult` pattern and keeps the two screens loosely coupled.

## Why This Matters for the Assignment Brief

The brief requires:
- "At least one RecyclerView or ListView with a custom Adapter" — the Pantry List Fragment's RecyclerView satisfies this.
- "A working navigation element (bottom navigation bar, toolbar menu, or navigation drawer)" — the BottomNavigationView satisfies this.
- "Correct use of Intents to navigate between screens and pass data" — used for Add/Edit and Recipe Detail.
- "A minimum of four (4) distinct screens/Activities (or Fragments navigated through a single host Activity)" — we have five: three Fragments + two Activities.