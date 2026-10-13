package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.os.Bundle;

/**
 * Entry point for the standalone calorie and nutrition tracker application.
 */
public final class FoodTrackerApp extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new TrackerGUI(this).show();
    }
}
