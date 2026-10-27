package com.collegediscovery.foodtracker;

/** A saved portion with a nutrient snapshot, independent of later catalog changes. */
public final class FoodLogEntry {
    public final long id;
    public final String name;
    public final Meal meal;
    public final double grams;
    public final NutritionSummary nutrition;

    public FoodLogEntry(long id, String name, Meal meal, double grams, NutritionSummary nutrition) {
        this.id = id;
        this.name = name;
        this.meal = meal;
        this.grams = grams;
        this.nutrition = nutrition;
    }
}
