package com.collegediscovery.foodtracker;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import java.io.File;
import java.util.List;

/** Dependency-free instrumented integration checks against the real bundled SQLite data. */
public final class RepositoryTestRunner extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle results = new Bundle();
        StringBuilder passed = new StringBuilder();
        Context isolated = new ContextWrapper(getTargetContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public File getDatabasePath(String name) {
                return new File(getCacheDir(), "repository-tests/" + name);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                    SQLiteDatabase.CursorFactory factory, android.database.DatabaseErrorHandler handler) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(), factory, handler);
            }
        };
        File file = isolated.getDatabasePath("FoodData.sqlite");
        // Only this test-specific cache database is reset; the application's user database is untouched.
        SQLiteDatabase.deleteDatabase(file);
        try {
            Food apple;
            try (FoodRepository repository = new FoodRepository(isolated)) {
                List<Food> foods = repository.search("apples, fuji");
                check(foods.size() == 1, "Fuji apple search");
                apple = foods.get(0);
                near(58.2, apple.getCalories(), "Atwater kcal priority");
                near(0.148, apple.getProtein(), "USDA protein");
                near(15.7, apple.getCarbs(), "USDA carbs");
                near(0.162, apple.getFat(), "USDA fat");
                for (Food match : repository.search("%")) {
                    check(match.getName().contains("%"), "Literal wildcard escaping");
                }
                check(repository.search("  ").isEmpty(), "Blank search");
                check(repository.search("no-such-food-98765").isEmpty(), "No-result search");
                passed.append("PASS: real USDA search, kcal selection, macros, wildcard and empty searches\n");

                repository.add(apple, Meal.LUNCH, "2026-10-26", 150);
                repository.add(apple, Meal.SNACKS, "2026-10-26", 50);
                repository.add(apple, Meal.DINNER, "2026-10-25", 100);
                NutritionSummary total = repository.getSummary("2026-10-26");
                near(116.4, total.calories, "Scaled calories");
                near(0.296, total.protein, "Scaled protein");
                near(31.4, total.carbs, "Scaled carbs");
                near(0.324, total.fat, "Scaled fat");
                near(58.2, repository.getSummary("2026-10-25").calories, "Day isolation");
                near(0, repository.getSummary("2026-10-27").calories, "Empty day");
                check(repository.getEntries("2026-10-26").get(0).meal == Meal.LUNCH, "Meal assignment");
                near(150, repository.getEntries("2026-10-26").get(0).grams, "Saved grams");
                for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 10001}) {
                    boolean rejected = false;
                    try { repository.add(apple, Meal.LUNCH, "2026-10-26", invalid); }
                    catch (IllegalArgumentException expected) { rejected = true; }
                    check(rejected, "Invalid grams " + invalid);
                }
                passed.append("PASS: portion scaling, meals, day isolation, invalid amounts\n");
            }
            try (FoodRepository reopened = new FoodRepository(isolated)) {
                near(116.4, reopened.getSummary("2026-10-26").calories, "Reopen persistence");
                reopened.remove(reopened.getEntries("2026-10-26").get(0).id);
                near(29.1, reopened.getSummary("2026-10-26").calories, "Removal updates totals");
                check(reopened.getEntries("2026-10-26").size() == 1, "One remaining entry");
                passed.append("PASS: persistence and removal\n");
            }
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                db.execSQL("ALTER TABLE food_log DROP COLUMN grams");
                db.setVersion(1);
            }
            try (FoodRepository upgraded = new FoodRepository(isolated)) {
                near(100, upgraded.getEntries("2026-10-26").get(0).grams, "Legacy portion default");
                near(29.1, upgraded.getSummary("2026-10-26").calories, "Migration preserves totals");
                passed.append("PASS: version 1 migration preserves entries\n");
            }
            results.putString("stream", "\n" + passed + "ALL REPOSITORY CHECKS PASSED\n");
            finish(-1, results);
        } catch (Throwable failure) {
            results.putString("stream", "\n" + passed + android.util.Log.getStackTraceString(failure));
            finish(0, results);
        } finally { SQLiteDatabase.deleteDatabase(file); }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void near(double expected, double actual, String message) {
        check(Math.abs(expected - actual) < 0.00001, message + ": expected " + expected + ", got " + actual);
    }
}
