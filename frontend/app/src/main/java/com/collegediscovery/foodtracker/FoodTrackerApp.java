package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.os.Bundle;

/**
 * Entry point for the standalone calorie and nutrition tracker application.
 */
public final class FoodTrackerApp extends Activity {
    private TrackerController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TrackerGUI tracker = new TrackerGUI(this);
        tracker.show();
        controller = new TrackerController(this, tracker);
    }

    @Override protected void onResume() {
        super.onResume();
        controller.onResume();
    }

    @Override protected void onPause() {
        controller.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) controller.close();
        super.onDestroy();
    }
}
