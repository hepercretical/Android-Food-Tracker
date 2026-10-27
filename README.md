# Calorie and Macro Tracker
## College-Discovery-Project
My individual project for my College Discovery Project class.

Native Java Android app for recording daily calories, protein, carbohydrates,
and fat. Search the offline USDA catalog, enter grams, choose a meal, and save
or remove entries. Logs persist on the phone and daily totals use its local date.

The Android build packages `backend/FoodData.sqlite` as an asset and copies it
to app-private storage on first use. `FoodRepository` handles SQLite access,
`TrackerController` handles application behavior, and `*GUI` classes bind views.
No server or internet connection is required to track food.

In VS Code, run **Terminal > Run Task > Android: run on emulator or phone** to
build the debug APK, launch `FoodTracker_Phone`, install, and open the app.
The APK is `frontend/app/build/outputs/apk/debug/app-debug.apk`.
See `frontend/TESTING.txt` for setup, manual checks, and SQLite integration tests.

All data for the database is sourced from the USDA's FoodData Central. (https://fdc.nal.usda.gov/)
