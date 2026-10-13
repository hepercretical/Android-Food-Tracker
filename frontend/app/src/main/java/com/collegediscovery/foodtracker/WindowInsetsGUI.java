package com.collegediscovery.foodtracker;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** Keeps the screen clear of system bars, display cutouts, and the keyboard. */
public final class WindowInsetsGUI {
    private WindowInsetsGUI() {
        // Stateless layout helper.
    }

    public static void apply(Activity activity, View root) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets bars = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets keyboard = windowInsets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
                return windowInsets;
            });
            root.requestApplyInsets();
        }
    }
}
