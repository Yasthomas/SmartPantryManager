# Smart Pantry Manager

An Android application that helps users reduce food waste by tracking the ingredients they already have at home and suggesting recipes they can cook using **strictly** those ingredients — no shopping trip required.

## App Description

The Smart Pantry Manager tackles household food waste at its source: the pantry. Users maintain a simple inventory of the ingredients they currently have — name, quantity, unit, and optional expiry date — and the app cross-references this inventory against a collection of recipes. The core value of the app is its **strict-matching rule**: a recipe is only suggested if the user already has *every single ingredient* it requires, in at least the required quantity. Partial matches are deliberately excluded, so the user never sees an "almost there" recipe masquerading as cookable. The app includes a pantry management screen with full CRUD (create, read, update, delete), a suggested-recipes screen driven by the matching algorithm, a recipe detail view, and a settings screen for user preferences such as expiring-soon alerts. Built entirely in Java using Android Studio.

## Database Choice: SQLite + Room

This app uses **SQLite via Android's Room Persistence Library** for local data storage.

**Why SQLite + Room (and not Firebase or PostgreSQL):**

- **Offline-first:** The app's core value is helping users cook from what they already have — often in the kitchen with unreliable Wi-Fi. A local database means the app works everywhere, including on a plane or in a basement pantry.
- **No backend required:** Firebase and PostgreSQL would each require a cloud account, an API key, or a hosted backend. SQLite runs entirely on-device, keeping the project scope focused on Android development rather than backend infrastructure.
- **Room's compile-time safety:** Room verifies SQL queries at compile time, catching typos and schema mismatches before the app runs — a significant advantage over raw `SQLiteOpenHelper`.
- **LiveData integration:** Room returns `LiveData` objects that automatically refresh the UI when the underlying data changes, eliminating manual refresh code and making the strict-matching algorithm reactive: the suggestions list updates the instant the user adds or removes a pantry item.
- **Course alignment:** SQLite (with Room or SQLiteOpenHelper) is the persistence approach covered in the module's database chapter, so this choice is directly justified by the curriculum.

The database schema consists of three tables: `pantry_items` (the user's current ingredients), `recipes` (recipe metadata), and `recipe_ingredients` (join table linking recipes to their required ingredients with quantities).

## Setup & Run Instructions

### Prerequisites

- **Android Studio** — [download here](https://developer.android.com/studio)
- **JDK 17** — bundled with Android Studio; no separate install required
- **Git** — [download here](https://git-scm.com/downloads)

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Yasthomas/SmartPantryManager.git
   cd SmartPantryManager
   ```

2. **Open in Android Studio:**

   - Launch Android Studio.
   - On the Welcome screen, click Open.
   - Navigate to the cloned SmartPantryManager folder and select it.
   - Click OK. Android Studio detects the Gradle project and begins syncing.

3. **Sync Gradle:**

   - Android Studio runs a Gradle sync automatically on first open. If not, click File → Sync Project with Gradle Files (or the elephant icon in the toolbar).
   - Wait for "Gradle sync finished" in the status bar. First sync may take 3–10 minutes as dependencies are downloaded.
   - If prompted to update Gradle or the Android Gradle Plugin, you can decline — the committed versions work.

4. Run the app:

   - Emulator: Open Device Manager (phone icon in toolbar) → start an AVD running API 24 or higher. Click the green ▶ Run button in the toolbar.
   - Physical device: Enable Developer Options and USB Debugging on your Android phone, connect via USB, select it from the device dropdown, and click ▶ Run.

5. **First run:**

   - The app creates the SQLite database on first launch and seeds it with 18 preloaded recipes.
   - Add pantry ingredients via the + button, then open the Suggested Recipes tab to see which recipes strictly match your pantry.

### Minimum Requirements

- **Minimum SDK** — API 24 (Android 7.0 Nougat)
- **Target SDK** — API 34 (Android 14)
- **Language** — Java
- **Build system** — Gradle (Groovy DSL)

### Project Structure
    ```text
    SmartPantryManager/
    ├── app/
    │   ├── src/main/java/com/thomas/smartpantry/   # Java source files
    │   ├── src/main/res/                             # Layouts, strings, themes
    │   └── src/main/AndroidManifest.xml
    ├── docs/                                         # Diagrams, report drafts, screenshots
    └── README.md
    ```

## Author

[Yaseen Thomas] — [402410593]
Mobile App Development 700 — Practical Assignment
[Richfield]
[October, 2026]

## License
Submitted as academic coursework. Not licensed for redistribution.