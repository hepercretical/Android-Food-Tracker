package com.collegediscovery.foodtracker;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** Reusable binding for one meal card; future card changes apply to every meal. */
public final class MealGUI {
    private final TextView title;
    private final TextView calories;
    private final TextView emptyMessage;
    private final TextView emptyDetail;
    private final Button addFoodButton;

    public MealGUI(View card, int titleResourceId) {
        // Scope lookups to the card because each card shares the same child IDs.
        title = card.findViewById(R.id.meal_title);
        calories = card.findViewById(R.id.meal_calories);
        emptyMessage = card.findViewById(R.id.meal_empty_message);
        emptyDetail = card.findViewById(R.id.meal_empty_detail);
        addFoodButton = card.findViewById(R.id.add_food);

        Context context = card.getContext();
        title.setText(titleResourceId);
        addFoodButton.setContentDescription(context.getString(
            R.string.add_food_to, context.getString(titleResourceId)));
    }

    public TextView getTitle() {
        return title;
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
