package com.collegediscovery.foodtracker;

/** Totals for the currently selected day. */
public final class NutritionSummary {
    public final double calories;
    public final double protein;
    public final double carbs;
    public final double fat;

    public NutritionSummary(double calories, double protein, double carbs, double fat) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }
}
