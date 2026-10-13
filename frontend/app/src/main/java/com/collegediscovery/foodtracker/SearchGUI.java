package com.collegediscovery.foodtracker;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/** Owns the food-search controls. Search behavior can be added here later. */
public final class SearchGUI {
    private final EditText searchField;
    private final Button searchButton;

    public SearchGUI(View root) {
        searchField = root.findViewById(R.id.food_search);
        searchButton = root.findViewById(R.id.search_button);
    }

    public EditText getSearchField() {
        return searchField;
    }

    public Button getSearchButton() {
        return searchButton;
    }
}
