package com.collegediscovery.foodtracker;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.util.List;
import java.util.function.Consumer;

/** Reusable binding for one meal card; future card changes apply to every meal. */
public final class MealGUI {
    private final TextView title;
    private final TextView calories;
    private final TextView emptyMessage;
    private final TextView emptyDetail;
    private final Button addFoodButton;
    private final LinearLayout entries;

    public MealGUI(View card, int titleResourceId) {
        // Scope lookups to the card because each card shares the same child IDs.
        title = card.findViewById(R.id.meal_title);
        calories = card.findViewById(R.id.meal_calories);
        emptyMessage = card.findViewById(R.id.meal_empty_message);
        emptyDetail = card.findViewById(R.id.meal_empty_detail);
        addFoodButton = card.findViewById(R.id.add_food);
        entries = card.findViewById(R.id.meal_entries);

        Context context = card.getContext();
        title.setText(titleResourceId);
        addFoodButton.setContentDescription(context.getString(
            R.string.add_food_to, context.getString(titleResourceId)));
    }

    public TextView getTitle() {
        return title;
    }

    public void render(List<FoodLogEntry> foods, Meal meal, Consumer<FoodLogEntry> onRemove) {
        Context context = title.getContext();
        entries.removeAllViews();
        double total = 0;
        for (FoodLogEntry food : foods) {
            if (food.meal != meal) continue;
            total += food.nutrition.calories;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            TextView details = new TextView(context);
            details.setTextAppearance(R.style.Body);
            details.setText(context.getString(R.string.entry_details, food.name, food.grams,
                food.nutrition.calories, food.nutrition.protein, food.nutrition.carbs, food.nutrition.fat));
            row.addView(details);
            Button remove = new Button(context);
            remove.setText(R.string.remove);
            remove.setContentDescription(context.getString(R.string.remove_named_food, food.name));
            remove.setOnClickListener(v -> onRemove.accept(food));
            row.addView(remove);
            entries.addView(row);
        }
        boolean empty = entries.getChildCount() == 0;
        emptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyDetail.setVisibility(empty ? View.VISIBLE : View.GONE);
        calories.setText(context.getString(R.string.meal_calories_value, total));
    }

    public TextView getCalories() {
        return calories;
    }

    public TextView getEmptyMessage() {
        return emptyMessage;
    }

    public TextView getEmptyDetail() {
        return emptyDetail;
    }

    public Button getAddFoodButton() {
        return addFoodButton;
    }
}
