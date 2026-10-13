package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.view.View;

/** Assembles the screen; each section owns its own view bindings. */
public final class TrackerGUI {
    private final Activity activity;
    private SearchGUI search;
    private DailyNutritionGUI dailyNutrition;
    private MealsGUI meals;

    public TrackerGUI(Activity activity) {
        this.activity = activity;
    }

    public void show() {
        activity.setContentView(R.layout.activity_tracker);
        View root = activity.findViewById(R.id.tracker_root);
        search = new SearchGUI(root);
        dailyNutrition = new DailyNutritionGUI(root);
        meals = new MealsGUI(root);
        WindowInsetsGUI.apply(activity, root);
    }

    /** Section bindings are available after show() has inflated the layout. */
    public SearchGUI getSearch() {
        return search;
    }

    public DailyNutritionGUI getDailyNutrition() {
        return dailyNutrition;
    }

    public MealsGUI getMeals() {
        return meals;
    }
}
