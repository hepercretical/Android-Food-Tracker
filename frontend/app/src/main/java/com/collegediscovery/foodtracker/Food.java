package com.collegediscovery.foodtracker;

/** A food returned by the USDA database, with nutrient values per 100 grams. */
public final class Food {
    private final long id;
    private final String name;
    private final double calories;
    private final double protein;
    private final double carbs;
    private final double fat;

    public Food(long id, String name, double calories, double protein, double carbs, double fat) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public double getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFat() { return fat; }
}
