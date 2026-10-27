package com.collegediscovery.foodtracker;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Owns the calorie-progress and macro-summary views, separate from calculations. */
public final class DailyNutritionGUI {
    private final ProgressBar calorieProgress;
    private final TextView calorieTotal;
    private final TextView calorieRemaining;
    private final TextView protein;
    private final TextView carbs;
    private final TextView fat;

    public DailyNutritionGUI(View root) {
        calorieProgress = root.findViewById(R.id.calorie_progress);
        calorieTotal = root.findViewById(R.id.calorie_total);
        calorieRemaining = root.findViewById(R.id.calorie_remaining);
        protein = root.findViewById(R.id.protein_total);
        carbs = root.findViewById(R.id.carbs_total);
        fat = root.findViewById(R.id.fat_total);
    }

    public ProgressBar getCalorieProgress() {
        return calorieProgress;
    }

    public void render(NutritionSummary summary) {
        android.content.Context context = calorieTotal.getContext();
        int calories = (int) Math.round(summary.calories);
        int goal = 2000;
        calorieProgress.setProgress(Math.min(calories, goal));
        calorieProgress.setContentDescription(context.getString(R.string.progress_value, calories, goal));
        calorieTotal.setText(context.getString(R.string.calories_value, calories, goal));
        calorieRemaining.setText(context.getString(calories > goal ? R.string.calories_over : R.string.calories_left, Math.abs(goal - calories)));
        protein.setText(context.getString(R.string.protein_value, summary.protein));
        carbs.setText(context.getString(R.string.carbs_value, summary.carbs));
        fat.setText(context.getString(R.string.fat_value, summary.fat));
    }

    public TextView getCalorieTotal() {
        return calorieTotal;
    }

    public TextView getCalorieRemaining() {
        return calorieRemaining;
    }

    public TextView getProtein() {
        return protein;
    }

    public TextView getCarbs() {
        return carbs;
    }

    public TextView getFat() {
        return fat;
    }
}
