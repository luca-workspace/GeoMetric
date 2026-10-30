package com.luca.geometric;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.google.android.material.color.DynamicColors;

/**
 * GeoMetricApplication
 *
 * Custom Application class registered in AndroidManifest.xml.
 * Initialized once by Android OS as soon as the application process starts,
 * remaining active for the entire application lifecycle.
 * Used for application-wide setups, such as applying custom font scaling preferences.
 */
public class GeoMetricApplication extends Application {

    // Shared preference storage constants
    public static final String PREFS        = "geometric_prefs";
    public static final String KEY_TEXT_SIZE = "text_size"; // "small" | "default" | "large"

    /**
     * Overrides attachBaseContext to apply user font scaling settings before any Activity starts.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(applyFontScale(base));
    }

    // ── Font scale configuration ──────────────────────────────────────────────

    /**
     * Reads saved text size preference and wraps the given Context with a modified Configuration fontScale.
     *
     * fontScale configurations:
     *   small   -> 0.85f  (15% smaller than standard text)
     *   default -> 1.00f  (standard system font scale)
     *   large   -> 1.20f  (20% larger text)
     *
     * @param context Base context to modify
     * @return New Context instance wrapped with updated font scale Configuration
     */
    public static Context applyFontScale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        String size = prefs.getString(KEY_TEXT_SIZE, "default");

        float scale;
        switch (size) {
            case "small":  scale = 0.85f; break;
            case "large":  scale = 1.20f; break;
            default:       scale = 1.00f; break;
        }

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.fontScale = scale;
        return context.createConfigurationContext(config);
    }

    /**
     * Saves chosen text size setting into SharedPreferences.
     * Called when the user changes text size options in SettingsFragment.
     *
     * @param context Calling context
     * @param size    Font size key ("small", "default", "large")
     */
    public static void saveFontSize(Context context, String size) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_TEXT_SIZE, size)
                .apply();
    }
}
