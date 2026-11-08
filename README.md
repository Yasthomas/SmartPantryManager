# Smart Pantry Manager

An Android app that tracks the ingredients in your pantry and suggests only those recipes whose
ingredients you already have — **all of them, in sufficient quantity** (the strict-matching rule).
A recipe never appears in the suggestions list while even one ingredient is missing, too small, or
in an incompatible unit. The app covers pantry CRUD (add / edit / delete with name, quantity, unit
and optional expiry), an expiring-soon badge, a strict suggestions list with an "Almost There"
section for recipes missing exactly one ingredient, a recipe detail screen, and a settings screen.
Built in Java with Android Studio.

**Database: SQLite via Room** — three tables (`pantry_items`, `recipes`, `recipe_ingredients`),
accessed through DAOs on a background executor, with 18 recipes seeded on first launch.

**JDK 17** — the project compiles with `sourceCompatibility`/`targetCompatibility` set to
`JavaVersion.VERSION_17`, using the JDK 17 bundled with Android Studio (Gradle 8.x, Android Gradle
Plugin 8.x, `compileSdk 34`, `targetSdk 34`, `minSdk 24`).

## Build and run

1. Open the `SmartPantryManager` folder in Android Studio and let Gradle sync finish
   (File → Sync Project with Gradle Files if it does not start automatically).
2. Create or select a device: Device Manager → any API 24+ emulator, or a physical phone with
   USB debugging enabled.
3. Press **Run** (Shift+F10 / the green ▶ button).

Command line:

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # install on a connected device/emulator
```

The first launch creates the SQLite database and seeds it with the 18 recipes; everything added
afterwards persists across app restarts.
