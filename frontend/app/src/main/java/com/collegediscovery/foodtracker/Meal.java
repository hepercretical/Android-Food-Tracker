package com.collegediscovery.foodtracker;

/** Stable storage values remain independent of translated screen labels. */
public enum Meal {
    BREAKFAST("Breakfast", R.string.breakfast), LUNCH("Lunch", R.string.lunch),
    DINNER("Dinner", R.string.dinner), SNACKS("Snacks", R.string.snacks);

    public final int label;
    private final String storedValue;

    Meal(String storedValue, int label) { this.storedValue = storedValue; this.label = label; }
    public String databaseValue() { return storedValue; }
    public static Meal fromDatabase(String value) {
        for (Meal meal : values()) if (meal.storedValue.equals(value)) return meal;
        throw new IllegalArgumentException("Unknown meal: " + value);
    }
}
