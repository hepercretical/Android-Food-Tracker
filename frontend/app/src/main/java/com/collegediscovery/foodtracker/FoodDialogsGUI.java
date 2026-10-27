package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.function.Consumer;

/** Search, portion entry, and removal dialogs. */
public final class FoodDialogsGUI {
    public interface OnAdd { void add(Food food, Meal meal, double grams); }
    private final Activity activity;
    private AlertDialog current;

    public FoodDialogsGUI(Activity activity) { this.activity = activity; }

    public void askSearch(Meal meal, Consumer<String> onSearch) {
        EditText field = new EditText(activity);
        field.setHint(R.string.search_hint);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout content = content();
        content.addView(field);
        current = new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.add_food_to, activity.getString(meal.label)))
            .setView(content).setPositiveButton(R.string.search, null).setNegativeButton(android.R.string.cancel, null).create();
        current.setOnShowListener(d -> current.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (field.getText().toString().trim().isEmpty()) { field.setError(activity.getString(R.string.enter_food)); return; }
            String query = field.getText().toString();
            current.dismiss();
            onSearch.accept(query);
        }));
        current.show();
    }

    public void showResults(List<Food> foods, Meal meal, OnAdd onAdd) {
        if (foods.isEmpty()) { Toast.makeText(activity, R.string.no_foods, Toast.LENGTH_LONG).show(); return; }
        CharSequence[] labels = new CharSequence[foods.size()];
        for (int i = 0; i < foods.size(); i++) {
            Food food = foods.get(i);
            labels[i] = activity.getString(R.string.food_result, food.getName(), food.getCalories());
        }
        current = new AlertDialog.Builder(activity).setTitle(R.string.choose_food).setItems(labels,
            (d, index) -> showPortion(foods.get(index), meal, onAdd)).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showPortion(Food food, Meal meal, OnAdd onAdd) {
        LinearLayout content = content();
        TextView nutrients = new TextView(activity);
        nutrients.setText(activity.getString(R.string.food_nutrients, food.getCalories(), food.getProtein(), food.getCarbs(), food.getFat()));
        content.addView(nutrients);
        TextView amountLabel = new TextView(activity);
        amountLabel.setText(R.string.amount_grams);
        content.addView(amountLabel);
        EditText grams = new EditText(activity);
        grams.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        grams.setSingleLine(true);
        grams.setText(NumberFormat.getNumberInstance().format(100));
        grams.setSelectAllOnFocus(true);
        grams.setContentDescription(activity.getString(R.string.amount_grams));
        content.addView(grams);
        TextView mealLabel = new TextView(activity);
        mealLabel.setText(R.string.meal_label);
        content.addView(mealLabel);
        Spinner mealPicker = new Spinner(activity);
        String[] mealLabels = new String[Meal.values().length];
        for (int i = 0; i < mealLabels.length; i++) mealLabels[i] = activity.getString(Meal.values()[i].label);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, mealLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mealPicker.setAdapter(adapter);
        if (meal != null) mealPicker.setSelection(meal.ordinal());
        content.addView(mealPicker);
        current = new AlertDialog.Builder(activity).setTitle(food.getName()).setView(content)
            .setPositiveButton(R.string.log_food, null).setNegativeButton(android.R.string.cancel, null).create();
        current.setOnShowListener(d -> current.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = grams.getText().toString().trim();
            ParsePosition position = new ParsePosition(0);
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            numberFormat.setGroupingUsed(false);
            Number parsed = numberFormat.parse(text, position);
            double amount = parsed == null ? Double.NaN : parsed.doubleValue();
            if (position.getIndex() != text.length() || !Double.isFinite(amount) || amount <= 0 || amount > 10000) {
                grams.setError(activity.getString(R.string.invalid_grams));
                return;
            }
            Meal chosen = Meal.values()[mealPicker.getSelectedItemPosition()];
            current.dismiss();
            onAdd.add(food, chosen, amount);
        }));
        current.show();
    }

    public void confirmRemove(FoodLogEntry entry, Runnable remove) {
        current = new AlertDialog.Builder(activity).setTitle(R.string.remove_food)
            .setMessage(activity.getString(R.string.remove_confirmation, entry.name))
            .setPositiveButton(R.string.remove, (d, which) -> remove.run()).setNegativeButton(android.R.string.cancel, null).show();
    }

    private LinearLayout content() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, padding / 2);
        return content;
    }

    public void close() { if (current != null) current.dismiss(); }
}
