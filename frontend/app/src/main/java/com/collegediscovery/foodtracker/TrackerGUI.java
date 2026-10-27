package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.view.View;
import java.util.List;
import java.util.function.Consumer;

/** Assembles view bindings; the controller owns application behavior. */
public final class TrackerGUI {
    private final Activity activity;
    private SearchGUI search;
    private DailyNutritionGUI dailyNutrition;
    private MealsGUI meals;

    public TrackerGUI(Activity activity) { this.activity = activity; }

    public void show() {
        activity.setContentView(R.layout.activity_tracker);
        View root = activity.findViewById(R.id.tracker_root);
        search = new SearchGUI(root);
        dailyNutrition = new DailyNutritionGUI(root);
        meals = new MealsGUI(root);
        WindowInsetsGUI.apply(activity, root);
    }

    public void render(NutritionSummary summary, List<FoodLogEntry> entries, Consumer<FoodLogEntry> onRemove) {
        dailyNutrition.render(summary);
        for (Meal meal : Meal.values()) meals.get(meal).render(entries, meal, onRemove);
    }

    public SearchGUI getSearch() { return search; }
    public DailyNutritionGUI getDailyNutrition() { return dailyNutrition; }
    public MealsGUI getMeals() { return meals; }
}
