# Navigation Architecture — Design Decisions

## Summary

The Smart Pantry Manager is built from **five Activities**, each with its own layout and toolbar:

| # | Screen | Class | Bottom nav? |
|---|---|---|---|
| 1 | Pantry list (launcher) | `ui.MainActivity` | yes |
| 2 | Add / Edit ingredient | `ui.AddEditItemActivity` | no |
| 3 | Suggested recipes | `ui.SuggestedRecipesActivity` | yes |
| 4 | Recipe detail | `ui.RecipeDetailActivity` | no |
| 5 | Settings | `ui.SettingsActivity` | yes |

Three of them are **top-level destinations** (screens 1, 3, 5), reached from the same
`BottomNavigationView`. The other two are **detail screens** that sit on top of them and are
closed with Back. Every transition is an `Intent`; no screen is a Fragment.

## Why BottomNavigationView (and not a Navigation Drawer or Toolbar Menu)?

| Option | Verdict | Reason |
|---|---|---|
| **BottomNavigationView** | **Chosen** | Ideal for 3–5 top-level destinations. On modern phones it sits in the thumb zone — easy to reach one-handed. Google's Material Design guidelines explicitly recommend it for apps with a small number of equally-important top-level screens. |
| Navigation Drawer | Rejected | Overkill for three destinations. Drawers are better suited to apps with 5+ top-level sections and less frequent switching between them. |
| Toolbar menu (overflow) | Rejected | Hides navigation behind a tap. Poor discoverability — the user has to guess what's behind the hamburger icon. |
| Tabs (TabLayout) | Rejected | Visually too similar to BottomNavigationView for a small app and less ergonomic (top of the screen is harder to reach one-handed). |

## Why Activities for Every Screen (and not Fragments in one host Activity)?

The three top-level screens share a bottom navigation bar, but they share no in-memory state:

- The pantry list reads what it needs from Room, through a `LiveData` query.
- The suggestions screen re-runs the matching engine from the stored pantry rows and recipes.
- Settings reads and writes its preferences through `SharedPreferences`.

Because the database is the single source of truth, a tab switch only has to re-read from it —
there is nothing to preserve in memory between screens. That makes a single-Activity Fragment
host unnecessary machinery: no `FragmentManager`, no `FragmentTransaction`, no `ViewModel`
scope to reason about, and no risk of a stale Fragment showing data the database has already
changed.

The trade-off is deliberate and understood: switching tabs starts a new Activity instead of
swapping a Fragment, so the screen is rebuilt. That costs one `inflate()` plus one short Room
query on a background thread — imperceptible here — and in exchange each screen keeps its own
toolbar, its own layout and its own lifecycle, which is easier to read, easier to reason about
and easier to demonstrate.

Each of the three top-level screens therefore inflates the same `BottomNavigationView` from its
own layout and wires it with the same small helper (`setupBottomNav` plus `openTopLevel`). That
duplication is three short methods; a Fragment host would replace it with a larger, more
indirect mechanism.

## Why Activities (and not dialogs or in-place editing) for Add/Edit and Recipe Detail?

Both screens:

- Are **modal** — the user is completing a specific task and will return once done
- Should participate in the **back stack** — pressing Back should dismiss them and return to the previous screen
- Do **not** appear in the bottom navigation bar
- Should **not** preserve state across tab switches (closing them discards any unsaved edits)

Activities are the natural fit: Android's back stack handles them automatically, and their
lifecycle is cleanly decoupled from the top-level screens.

## Screen-to-Screen Transitions

| From | To | Trigger | Mechanism |
|---|---|---|---|
| App launch | Pantry list | Launcher icon | `MainActivity`, the only exported Activity |
| Pantry list | Add ingredient | FAB (+) tap | `Intent` to `AddEditItemActivity` with no extras |
| Pantry list | Edit ingredient | List item tap | `Intent` to `AddEditItemActivity` with `EXTRA_ITEM_ID` |
| Add / Edit | Pantry list | Save or Back | `finish()`; the `LiveData` list refreshes itself |
| Pantry list | Suggested recipes | Bottom nav tab 2 | `Intent` with `CLEAR_TOP` + `SINGLE_TOP`, then `finish()` |
| Suggested recipes | Recipe detail | List item tap | `Intent` to `RecipeDetailActivity` with `EXTRA_RECIPE_ID` |
| Recipe detail | Suggested recipes | Back | `finish()` returns to the suggestions screen |
| Suggested recipes | Settings | Bottom nav tab 3 | `Intent` with `CLEAR_TOP` + `SINGLE_TOP`, then `finish()` |

Taps on the tab the user is already on are ignored, so a screen is never rebuilt for nothing.

## Passing Data Between Screens

Only two values ever travel in an `Intent`, and both are row ids — never the rows themselves:

- **Pantry list → Add / Edit (edit mode):** `intent.putExtra(AddEditItemActivity.EXTRA_ITEM_ID, id)`.
  The form then loads the row by id, so it always edits current data.
- **Suggested recipes → Recipe detail:** `intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, id)`.

Nothing is passed back. Instead:

- The pantry list observes `LiveData` from Room, so any `insert`/`update`/`delete` propagates
  to the list automatically — no result code, no `startActivityForResult`, no refresh button.
- The suggestions screen re-runs the matching engine in `onResume()`, so returning from an edit
  always shows the effect of that edit and nothing is ever cached.
- The pantry list re-reads its badge settings in `onResume()`, so a threshold changed on the
  Settings screen is visible the moment the user comes back.

## Back-Stack Behaviour

Top-level switches use `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` followed by
`finish()`:

- `CLEAR_TOP` removes anything stacked above an existing instance of the target screen
- `SINGLE_TOP` delivers the `Intent` to that instance instead of creating a second one
- `finish()` closes the screen being left, so exactly one instance of each top-level screen is
  alive at a time

Without those flags, Pantry → Suggestions → Pantry → Suggestions would build an ever-growing
stack and Back would walk through every visit. With them, Back from any top-level screen simply
leaves the app, which is what a three-tab bar implies.

## Why This Matters for the Assignment Brief

The brief requires:

- *"At least one RecyclerView or ListView with a custom Adapter"* — the pantry list and both
  suggestion lists use a `RecyclerView` with a custom `RecyclerView.Adapter` (`PantryAdapter`,
  `RecipeAdapter`).
- *"A working navigation element (bottom navigation bar, toolbar menu, or navigation drawer)"* —
  the `BottomNavigationView` shared by the three top-level screens.
- *"Correct use of Intents to navigate between screens and pass data"* — every transition in the
  table above, with row ids carried in extras.
- *"A minimum of four (4) distinct screens/Activities"* — five Activities, all declared in
  `AndroidManifest.xml`, with only `MainActivity` exported.
