package com.collegediscovery.foodtracker;

import android.view.View;

/** Groups the day's meal cards and assigns their labels. */
public final class MealsGUI {
    private final MealGUI breakfast;
    private final MealGUI lunch;
    private final MealGUI dinner;
    private final MealGUI snacks;

    public MealsGUI(View root) {
        breakfast = new MealGUI(root.findViewById(R.id.breakfast), R.string.breakfast);
        lunch = new MealGUI(root.findViewById(R.id.lunch), R.string.lunch);
        dinner = new MealGUI(root.findViewById(R.id.dinner), R.string.dinner);
        snacks = new MealGUI(root.findViewById(R.id.snacks), R.string.snacks);
    }

    public MealGUI getBreakfast() {
        return breakfast;
    }

    public MealGUI get(Meal meal) {
        switch (meal) {
            case BREAKFAST: return breakfast;
            case LUNCH: return lunch;
            case DINNER: return dinner;
            case SNACKS: return snacks;
            default: throw new IllegalArgumentException("Unknown meal");
        }
    }

    public MealGUI getLunch() {
        return lunch;
    }

    public MealGUI getDinner() {
        return dinner;
    }

    public MealGUI getSnacks() {
        return snacks;
    }
}
