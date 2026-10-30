package com.luca.geometric;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SettingsFragment
 *
 * Provides application configuration controls:
 *   - Displaying app build version string
 *   - Text size scaling toggle (small / default / large) with live activity recreation
 *   - Vibration feedback toggle switch
 *   - Sound effects toggle switch
 *   - Export user progress to JSON file in Downloads directory via MediaStore API
 *   - Import user progress from selected JSON file via Activity Result File Picker
 *   - Reset all progress and re-initialize database
 */
public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;

    // File picker contract launcher for importing progress backup JSON file
    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                importProgress(uri);
            });

    // SharedPreferences key constants
    private static final String KEY_TEXT_SIZE           = GeoMetricApplication.KEY_TEXT_SIZE;
    private static final String KEY_VIBRATION           = "vibration_enabled";
    private static final String KEY_SOUNDS              = "sounds_enabled";
    private static final String KEY_XP                  = "xp";
    private static final String KEY_STREAK              = "streak_days";
    private static final String KEY_PROGRESS_CAPITALS   = "progress_capitals";
    private static final String KEY_PROGRESS_FLAG       = "progress_flags";
    private static final String KEY_PROGRESS_MAP        = "progress_maps";
    private static final String KEY_PROGRESS_LAND       = "progress_monuments";
    private static final String KEY_STARS_CAPITALS      = "stars_capitals";
    private static final String KEY_STARS_FLAGS         = "stars_flags";
    private static final String KEY_STARS_MAPS          = "stars_maps";
    private static final String KEY_STARS_MONUMENTS     = "stars_monuments";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate settings layout
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(GeoMetricApplication.PREFS, Context.MODE_PRIVATE);

        setupVersionLabel(view);
        setupTextSizeToggle(view);
        setupVibrationSwitch(view);
        setupSoundsSwitch(view);
        setupExportCard(view);
        setupImportCard(view);
        setupResetCard(view);
    }

    // VERSION LABEL
    /**
     * Retrieves application version name from PackageManager and displays it in TextView.
     */
    private void setupVersionLabel(View view) {
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            ((TextView) view.findViewById(R.id.tv_version))
                    .setText("Version " + versionName);
        } catch (Exception ignored) {}
    }

    // TEXT SIZE TOGGLE
    /**
     * Initializes MaterialButtonToggleGroup for selecting small, default, or large text scaling.
     * Recreates the hosting Activity when selection changes so new font scaling takes effect.
     */
    private void setupTextSizeToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_text_size);

        // Restore currently selected text size preference
        String current = prefs.getString(KEY_TEXT_SIZE, "default");
        switch (current) {
            case "small":   toggle.check(R.id.btn_size_small);   break;
            case "large":   toggle.check(R.id.btn_size_large);   break;
            default:        toggle.check(R.id.btn_size_default); break;
        }

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            String size;
            if      (checkedId == R.id.btn_size_small)   size = "small";
            else if (checkedId == R.id.btn_size_large)   size = "large";
            else                                         size = "default";

            // Save new font size setting
            GeoMetricApplication.saveFontSize(requireContext(), size);

            // Recreate hosting activity immediately to apply font scaling configuration
            requireActivity().recreate();
        });
    }

    // VIBRATION SWITCH
    /**
     * Configures vibration toggle switch and attaches check listener.
     */
    private void setupVibrationSwitch(View view) {
        MaterialSwitch sw = view.findViewById(R.id.switch_vibration);
        sw.setChecked(prefs.getBoolean(KEY_VIBRATION, true));

        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
            if (isChecked) vibrateConfirm(); // Short confirmation vibration
        });
    }

    /**
     * Utility method to trigger haptic vibration feedback.
     */
    public static void vibrate(Context context, long milliseconds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager)
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vm.getDefaultVibrator().vibrate(
                        VibrationEffect.createOneShot(milliseconds,
                                VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(milliseconds,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(milliseconds);
                }
            }
        }
    }

    private void vibrateConfirm() { vibrate(requireContext(), 40); }

    // SOUNDS SWITCH
    /**
     * Configures sound effects toggle switch.
     */
    private void setupSoundsSwitch(View view) {
        MaterialSwitch sw = view.findViewById(R.id.switch_sound);
        sw.setChecked(prefs.getBoolean(KEY_SOUNDS, true));
        sw.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(KEY_SOUNDS, isChecked).apply());
    }

    // EXPORT PROGRESS
    private void setupExportCard(View view) {
        MaterialCardView card = view.findViewById(R.id.card_export);
        card.setOnClickListener(v -> exportProgress());
    }

    /**
     * Exports SharedPreferences user stats and database progress into a JSON file saved in Downloads directory via MediaStore API.
     */
    private void exportProgress() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put(KEY_XP,                prefs.getInt(KEY_XP,                0));
                json.put(KEY_STREAK,            prefs.getInt(KEY_STREAK,            0));
                json.put(KEY_PROGRESS_CAPITALS, prefs.getInt(KEY_PROGRESS_CAPITALS, 0));
                json.put(KEY_PROGRESS_FLAG,     prefs.getInt(KEY_PROGRESS_FLAG,     0));
                json.put(KEY_PROGRESS_MAP,      prefs.getInt(KEY_PROGRESS_MAP,      0));
                json.put(KEY_PROGRESS_LAND,     prefs.getInt(KEY_PROGRESS_LAND,     0));
                json.put(KEY_STARS_CAPITALS,    prefs.getInt(KEY_STARS_CAPITALS,    0));
                json.put(KEY_STARS_FLAGS,       prefs.getInt(KEY_STARS_FLAGS,       0));
                json.put(KEY_STARS_MAPS,        prefs.getInt(KEY_STARS_MAPS,        0));
                json.put(KEY_STARS_MONUMENTS,   prefs.getInt(KEY_STARS_MONUMENTS,   0));
                json.put(KEY_TEXT_SIZE,         prefs.getString(KEY_TEXT_SIZE,     "default"));
                json.put(KEY_VIBRATION,         prefs.getBoolean(KEY_VIBRATION,     true));
                json.put(KEY_SOUNDS,            prefs.getBoolean(KEY_SOUNDS,        true));

                // Export questions with wrong counts
                GeoMetricDatabase db = GeoMetricDatabase.getInstance(requireContext());
                List<Question> missedQuestions = db.questionDao().getQuestionsWithWrongCounts();
                JSONArray questionsArray = new JSONArray();
                for (Question q : missedQuestions) {
                    JSONObject qJson = new JSONObject();
                    qJson.put("id", q.getId());
                    qJson.put("wrongCount", q.getWrongCount());
                    questionsArray.put(qJson);
                }
                json.put("questions_progress", questionsArray);

                // Export block progress
                List<BlockProgress> blocks = db.blockProgressDao().getAllBlocks();
                JSONArray blocksArray = new JSONArray();
                for (BlockProgress b : blocks) {
                    JSONObject bJson = new JSONObject();
                    bJson.put("category", b.category);
                    bJson.put("difficulty", b.difficulty);
                    bJson.put("blockIndex", b.blockIndex);
                    bJson.put("isUnlocked", b.isUnlocked);
                    bJson.put("bestScore", b.bestScore);
                    bJson.put("completionCount", b.completionCount);
                    bJson.put("questionCount", b.questionCount);
                    blocksArray.put(bJson);
                }
                json.put("block_progress", blocksArray);

                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, "GeoMetric_backup.json");
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                cv.put(MediaStore.Downloads.IS_PENDING, 1);

                android.net.Uri uri = requireContext().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);

                if (uri != null) {
                    try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    cv.clear();
                    cv.put(MediaStore.Downloads.IS_PENDING, 0);
                    requireContext().getContentResolver().update(uri, cv, null, null);

                    androidx.fragment.app.FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (getView() != null && getActivity() != null) {
                                View anchor = getActivity().findViewById(R.id.bottom_nav_card);
                                Snackbar snack = Snackbar.make(getView(), "Backup saved to Downloads", Snackbar.LENGTH_LONG);
                                if (anchor != null) snack.setAnchorView(anchor);
                                snack.show();
                            }
                        });
                    }
                }

            } catch (Exception e) {
                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (getView() != null && getActivity() != null) {
                            View anchor = getActivity().findViewById(R.id.bottom_nav_card);
                            Snackbar snack = Snackbar.make(getView(), "Export failed: " + e.getMessage(), Snackbar.LENGTH_LONG);
                            if (anchor != null) snack.setAnchorView(anchor);
                            snack.show();
                        }
                    });
                }
            }
        });
    }

    // IMPORT PROGRESS
    private void setupImportCard(View view) {
        MaterialCardView card = view.findViewById(R.id.card_import);
        card.setOnClickListener(v -> importLauncher.launch(new String[]{"application/json"}));
    }

    /**
     * Imports progress backup JSON file selected via Document Picker into SharedPreferences and Room database.
     */
    private void importProgress(android.net.Uri uri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // 1. Read the JSON file content from the provided URI
                InputStream in = requireContext().getContentResolver().openInputStream(uri);
                if (in == null) return;

                byte[] bytes = new byte[in.available()];
                in.read(bytes);
                in.close();

                JSONObject json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));

                // 2. Restore basic stats, preferences, and global stars into SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                if (json.has(KEY_XP))                editor.putInt(KEY_XP,                json.getInt(KEY_XP));
                if (json.has(KEY_STREAK))            editor.putInt(KEY_STREAK,            json.getInt(KEY_STREAK));
                if (json.has(KEY_PROGRESS_CAPITALS)) editor.putInt(KEY_PROGRESS_CAPITALS, json.getInt(KEY_PROGRESS_CAPITALS));
                if (json.has(KEY_PROGRESS_FLAG))     editor.putInt(KEY_PROGRESS_FLAG,     json.getInt(KEY_PROGRESS_FLAG));
                if (json.has(KEY_PROGRESS_MAP))      editor.putInt(KEY_PROGRESS_MAP,      json.getInt(KEY_PROGRESS_MAP));
                if (json.has(KEY_PROGRESS_LAND))     editor.putInt(KEY_PROGRESS_LAND,     json.getInt(KEY_PROGRESS_LAND));
                if (json.has(KEY_STARS_CAPITALS))    editor.putInt(KEY_STARS_CAPITALS,    json.getInt(KEY_STARS_CAPITALS));
                if (json.has(KEY_STARS_FLAGS))       editor.putInt(KEY_STARS_FLAGS,       json.getInt(KEY_STARS_FLAGS));
                if (json.has(KEY_STARS_MAPS))        editor.putInt(KEY_STARS_MAPS,        json.getInt(KEY_STARS_MAPS));
                if (json.has(KEY_STARS_MONUMENTS))   editor.putInt(KEY_STARS_MONUMENTS,   json.getInt(KEY_STARS_MONUMENTS));
                if (json.has(KEY_TEXT_SIZE))         editor.putString(KEY_TEXT_SIZE,      json.getString(KEY_TEXT_SIZE));
                if (json.has(KEY_VIBRATION))         editor.putBoolean(KEY_VIBRATION,     json.getBoolean(KEY_VIBRATION));
                if (json.has(KEY_SOUNDS))            editor.putBoolean(KEY_SOUNDS,        json.getBoolean(KEY_SOUNDS));
                editor.apply();

                GeoMetricDatabase db = GeoMetricDatabase.getInstance(requireContext());

                // 3. Restore spaced-repetition statistics (wrong answer count for each question)
                if (json.has("questions_progress")) {
                    JSONArray questionsArray = json.getJSONArray("questions_progress");
                    
                    // Clear the current error history before applying the backup's history
                    db.questionDao().resetAllWrongCounts();
                    for (int i = 0; i < questionsArray.length(); i++) {
                        JSONObject qJson = questionsArray.getJSONObject(i);
                        int id = qJson.getInt("id");
                        int wrongCount = qJson.getInt("wrongCount");
                        db.questionDao().updateWrongCount(id, wrongCount);
                    }
                }

                // 4. Restore the granular progress (unlocked state, best scores) for each specific block
                if (json.has("block_progress")) {
                    JSONArray blocksArray = json.getJSONArray("block_progress");
                    
                    // Wipe the current block table to eliminate any corrupted ghost blocks 
                    // from early app versions, then regenerate the clean, mathematically correct structure
                    db.blockProgressDao().deleteAll();
                    GeoMetricDatabase.initBlockProgress(db);
                    
                    // Inject the imported progress data into the newly generated clean blocks
                    for (int i = 0; i < blocksArray.length(); i++) {
                        JSONObject bJson = blocksArray.getJSONObject(i);
                        String cat = bJson.getString("category");
                        int diff = bJson.getInt("difficulty");
                        int bIndex = bJson.getInt("blockIndex");
                        
                        // Retrieve the clean block from the DB and apply the saved scores and unlock state
                        BlockProgress existing = db.blockProgressDao().getBlock(cat, diff, bIndex);
                        if (existing != null) {
                            existing.isUnlocked = bJson.getBoolean("isUnlocked");
                            existing.bestScore = bJson.getInt("bestScore");
                            if (bJson.has("completionCount")) {
                                existing.completionCount = bJson.getInt("completionCount");
                            }
                            db.blockProgressDao().update(existing);
                        }
                    }
                }

                // 5. Notify the user of a successful import on the main UI thread
                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (getView() != null && getActivity() != null) {
                            View anchor = getActivity().findViewById(R.id.bottom_nav_card);
                            Snackbar snack = Snackbar.make(getView(), "Backup imported successfully", Snackbar.LENGTH_LONG);
                            if (anchor != null) snack.setAnchorView(anchor);
                            snack.show();
                        }
                    });
                }

            } catch (Exception e) {
                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (getView() != null && getActivity() != null) {
                            View anchor = getActivity().findViewById(R.id.bottom_nav_card);
                            Snackbar snack = Snackbar.make(getView(), "Import failed: " + e.getMessage(), Snackbar.LENGTH_LONG);
                            if (anchor != null) snack.setAnchorView(anchor);
                            snack.show();
                        }
                    });
                }
            }
        });
    }

    // RESET PROGRESS
    private void setupResetCard(View view) {
        MaterialCardView card = view.findViewById(R.id.card_reset);
        card.setOnClickListener(v -> showResetDialog());
    }

    /**
     * Displays confirmation dialog before wiping user data and progress.
     */
    private void showResetDialog() {
        new AlertDialog.Builder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Reset all progress?")
                .setMessage("This will permanently delete your XP, streaks, and progress in all categories. This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> resetAllData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Clears SharedPreferences stats and re-initializes database tables.
     */
    private void resetAllData() {
        prefs.edit()
                .remove(KEY_XP)
                .remove(KEY_STREAK)
                .remove(KEY_PROGRESS_CAPITALS)
                .remove(KEY_PROGRESS_FLAG)
                .remove(KEY_PROGRESS_MAP)
                .remove(KEY_PROGRESS_LAND)
                .remove(KEY_STARS_CAPITALS)
                .remove(KEY_STARS_FLAGS)
                .remove(KEY_STARS_MAPS)
                .remove(KEY_STARS_MONUMENTS)
                .remove(KEY_VIBRATION)
                .remove(KEY_SOUNDS)
                .remove("last_played_date")
                .apply();

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() ->
                GeoMetricDatabase.resetAndReinit(requireContext())
        );

        if (getView() != null) {
            View anchor = getActivity() != null ? getActivity().findViewById(R.id.bottom_nav_card) : null;
            Snackbar snack = Snackbar.make(getView(), "Progress reset", Snackbar.LENGTH_SHORT);
            if (anchor != null) snack.setAnchorView(anchor);
            snack.show();
        }
    }
}
