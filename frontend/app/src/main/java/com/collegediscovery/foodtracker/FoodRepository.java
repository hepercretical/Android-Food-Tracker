package com.collegediscovery.foodtracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Local data layer for USDA search results and the user's daily food log. */
public final class FoodRepository extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "FoodData.sqlite";
    private static final int DATABASE_VERSION = 2;
    private static final String LOG_TABLE = "food_log";

    public FoodRepository(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        copyDatabaseIfNeeded(context.getApplicationContext());
    }

    private void copyDatabaseIfNeeded(Context context) {
        File database = context.getDatabasePath(DATABASE_NAME);
        if (database.exists()) return;
        File parent = database.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IllegalStateException("Unable to create the database directory");
        }
        File staging = new File(parent, DATABASE_NAME + ".install");
        try (InputStream input = context.getAssets().open(DATABASE_NAME);
             FileOutputStream output = new FileOutputStream(staging)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to install the food database", e);
        }
        if (!staging.renameTo(database)) {
            throw new IllegalStateException("Unable to finish installing the food database");
        }
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + LOG_TABLE + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, food_id INTEGER NOT NULL, " +
            "food_name TEXT NOT NULL, meal TEXT NOT NULL, log_date TEXT NOT NULL, " +
            "calories REAL NOT NULL, protein REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, " +
            "grams REAL NOT NULL DEFAULT 100 CHECK (grams > 0))");
        db.execSQL("CREATE INDEX IF NOT EXISTS food_log_date ON food_log(log_date)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The bundled USDA database is version 0. Version 1 added the user's log.
        if (oldVersion < 1) {
            onCreate(db);
        } else if (oldVersion < 2) {
            db.execSQL("ALTER TABLE food_log ADD COLUMN grams REAL NOT NULL DEFAULT 100");
            db.execSQL("CREATE INDEX IF NOT EXISTS food_log_date ON food_log(log_date)");
        }
    }

    public List<Food> search(String text) {
        List<Food> foods = new ArrayList<>();
        if (text.trim().isEmpty()) return foods;
        String term = "%" + text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String sql = "SELECT r.__row_id, r.description, " +
            "COALESCE(MAX(CASE WHEN n.nutrient_id='1008' AND n.nutrient_unitName='kcal' THEN CAST(n.amount AS REAL) END), " +
            "MAX(CASE WHEN n.nutrient_id='2048' AND n.nutrient_unitName='kcal' THEN CAST(n.amount AS REAL) END), " +
            "MAX(CASE WHEN n.nutrient_id='2047' AND n.nutrient_unitName='kcal' THEN CAST(n.amount AS REAL) END), " +
            "MAX(CASE WHEN n.nutrient_id='1062' AND n.nutrient_unitName='kJ' THEN CAST(n.amount AS REAL)/4.184 END)), " +
            "MAX(CASE WHEN n.nutrient_id='1003' THEN CAST(n.amount AS REAL) END), " +
            "MAX(CASE WHEN n.nutrient_id='1005' THEN CAST(n.amount AS REAL) END), " +
            "MAX(CASE WHEN n.nutrient_id='1004' THEN CAST(n.amount AS REAL) END) " +
            "FROM json_records r LEFT JOIN json_foundation_foods_food_nutrients n " +
            "ON n.__parent_id = r.__row_id WHERE r.description LIKE ? ESCAPE '\\' " +
            "GROUP BY r.__row_id, r.description ORDER BY r.description LIMIT 50";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{term})) {
            while (cursor.moveToNext()) {
                // Omit incomplete foods rather than presenting unknown nutrients as zero.
                if (cursor.isNull(2) || cursor.isNull(3) || cursor.isNull(4) || cursor.isNull(5)) continue;
                foods.add(new Food(cursor.getLong(0), cursor.getString(1), cursor.getDouble(2),
                    cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5)));
            }
        }
        return foods;
    }

    public void add(Food food, Meal meal, String date, double grams) {
        if (!Double.isFinite(grams) || grams <= 0 || grams > 10000) {
            throw new IllegalArgumentException("Amount must be between 0 and 10,000 grams");
        }
        LocalDate.parse(date);
        double factor = grams / 100.0;
        getWritableDatabase().execSQL("INSERT INTO " + LOG_TABLE +
            " (food_id, food_name, meal, log_date, calories, protein, carbs, fat, grams) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            new Object[]{food.getId(), food.getName(), meal.databaseValue(), date,
                food.getCalories()*factor, food.getProtein()*factor, food.getCarbs()*factor, food.getFat()*factor, grams});
    }

    public List<FoodLogEntry> getEntries(String date) {
        List<FoodLogEntry> entries = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,food_name,meal,grams,calories,protein,carbs,fat FROM food_log WHERE log_date=? ORDER BY id", new String[]{date})) {
            while (c.moveToNext()) entries.add(new FoodLogEntry(c.getLong(0), c.getString(1),
                Meal.fromDatabase(c.getString(2)), c.getDouble(3),
                new NutritionSummary(c.getDouble(4),c.getDouble(5),c.getDouble(6),c.getDouble(7))));
        }
        return entries;
    }

    public void remove(long id) {
        getWritableDatabase().delete(LOG_TABLE, "id=?", new String[]{Long.toString(id)});
    }

    public NutritionSummary getSummary(String date) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(calories),0), COALESCE(SUM(protein),0), COALESCE(SUM(carbs),0), COALESCE(SUM(fat),0) FROM " + LOG_TABLE + " WHERE log_date = ?", new String[]{date})) {
            c.moveToFirst();
            return new NutritionSummary(c.getDouble(0), c.getDouble(1), c.getDouble(2), c.getDouble(3));
        }
    }
}
