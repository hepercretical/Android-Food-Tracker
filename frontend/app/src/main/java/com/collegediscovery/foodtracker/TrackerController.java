package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Toast;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates persistence and view updates on their respective threads. */
public final class TrackerController {
    private final Activity activity;
    private final TrackerGUI gui;
    private final FoodDialogsGUI dialogs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private FoodRepository repository;
    private volatile boolean closed;
    private boolean searching;
    private final Runnable midnightRefresh = new Runnable() {
        @Override public void run() { refresh(); scheduleMidnight(); }
    };

    public TrackerController(Activity activity, TrackerGUI gui) {
        this.activity = activity;
        this.gui = gui;
        dialogs = new FoodDialogsGUI(activity);
        gui.getSearch().getSearchButton().setOnClickListener(v -> search(null));
        gui.getSearch().getSearchField().setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            search(null);
            return true;
        });
        for (Meal meal : Meal.values()) gui.getMeals().get(meal).getAddFoodButton()
            .setOnClickListener(v -> dialogs.askSearch(meal, query -> search(query, meal)));
    }

    private void search(Meal meal) {
        search(gui.getSearch().getSearchField().getText().toString(), meal);
    }

    private void search(String query, Meal meal) {
        if (query.trim().isEmpty()) {
            gui.getSearch().getSearchField().setError(activity.getString(R.string.enter_food));
            return;
        }
        if (searching) return;
        searching = true;
        gui.getSearch().getSearchButton().setEnabled(false);
        execute(() -> {
            List<Food> foods = database().search(query);
            post(() -> {
                searching = false;
                gui.getSearch().getSearchButton().setEnabled(true);
                InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(gui.getSearch().getSearchField().getWindowToken(), 0);
                gui.getSearch().getSearchField().clearFocus();
                dialogs.showResults(foods, meal, (food, chosenMeal, grams) -> {
                    String date = LocalDate.now().toString();
                    execute(() -> { database().add(food, chosenMeal, date, grams); loadDay(); });
                });
            });
        });
    }

    private FoodRepository database() {
        if (repository == null) repository = new FoodRepository(activity.getApplicationContext());
        return repository;
    }

    private void loadDay() {
        String date = LocalDate.now().toString();
        NutritionSummary summary = database().getSummary(date);
        List<FoodLogEntry> entries = database().getEntries(date);
        post(() -> gui.render(summary, entries, entry -> dialogs.confirmRemove(entry, () -> execute(() -> {
            database().remove(entry.id);
            loadDay();
        }))));
    }

    public void refresh() { execute(this::loadDay); }
    public void onResume() { refresh(); scheduleMidnight(); }
    public void onPause() { main.removeCallbacks(midnightRefresh); }

    private void scheduleMidnight() {
        main.removeCallbacks(midnightRefresh);
        long delay = java.time.Duration.between(java.time.ZonedDateTime.now(),
            LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())).toMillis();
        main.postDelayed(midnightRefresh, Math.max(1000, delay));
    }

    private void execute(Runnable action) {
        if (closed) return;
        worker.execute(() -> {
            try { action.run(); }
            catch (RuntimeException error) {
                Log.e("FoodTracker", "Database operation failed", error);
                post(() -> {
                    searching = false;
                    gui.getSearch().getSearchButton().setEnabled(true);
                    Toast.makeText(activity, R.string.database_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void post(Runnable action) {
        main.post(() -> { if (!closed && !activity.isFinishing() && !activity.isDestroyed()) action.run(); });
    }

    public void close() {
        closed = true;
        main.removeCallbacksAndMessages(null);
        dialogs.close();
        // Queue closing after pending writes so rotation cannot close an active database.
        worker.execute(() -> { if (repository != null) repository.close(); });
        worker.shutdown();
    }
}
