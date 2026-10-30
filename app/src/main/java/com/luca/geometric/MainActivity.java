package com.luca.geometric;

import static com.luca.geometric.GeoMetricApplication.applyFontScale;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

/**
 * MainActivity
 *
 * Primary entry point activity housing the fragment container and bottom navigation bar.
 * Handles Edge-to-Edge window inset padding, bottom navigation switching between
 * HomeFragment and SettingsFragment, and preserving current tab selection during activity recreations.
 */
public class MainActivity extends AppCompatActivity {

    // Static variable to retain selected navigation tab state across Activity recreate() calls
    // (e.g., when font scale size is changed in SettingsFragment)
    private static boolean sHomeSelected = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure activity window to draw edge-to-edge behind system status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // Apply WindowInsets padding to avoid UI elements overlapping with status or navigation bars
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        // Initialize custom bottom navigation bar buttons
        View btnHome = findViewById(R.id.nav_btn_home);
        View btnSettings = findViewById(R.id.nav_btn_settings);

        // Set Home button listener
        btnHome.setOnClickListener(v -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof HomeFragment) return;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment()).commit();
            updateNavSelection(true);
        });

        // Set Settings button listener
        btnSettings.setOnClickListener(v -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof SettingsFragment) return;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment()).commit();
            updateNavSelection(false);
        });

        // Load initial fragment if activity is launched for the first time
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment()).commit();
            updateNavSelection(true);
        }

        // Restore active navigation selection state
        updateNavSelection(sHomeSelected);
    }

    /**
     * Updates navigation button text and icon tint colors to visually highlight the active tab.
     *
     * @param homeSelected True if Home tab is selected, false if Settings tab is selected.
     */
    private void updateNavSelection(boolean homeSelected) {
        sHomeSelected = homeSelected;
        int activeColor   = ContextCompat.getColor(this, R.color.accent_purple);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        MaterialButton btnHome     = findViewById(R.id.nav_btn_home);
        MaterialButton btnSettings = findViewById(R.id.nav_btn_settings);

        btnHome.setTextColor(homeSelected ? activeColor : inactiveColor);
        btnHome.setIconTint(ColorStateList.valueOf(homeSelected ? activeColor : inactiveColor));

        btnSettings.setTextColor(homeSelected ? inactiveColor : activeColor);
        btnSettings.setIconTint(ColorStateList.valueOf(homeSelected ? inactiveColor : activeColor));
    }

    /**
     * Overrides attachBaseContext to inject custom user font scaling preferences into configuration context.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(applyFontScale(base));
    }
}
