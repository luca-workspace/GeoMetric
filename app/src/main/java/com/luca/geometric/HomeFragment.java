package com.luca.geometric;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * HomeFragment
 *
 * Dashboard screen displaying overall user statistics:
 *   - Current XP total in header
 *   - 10-day activity streak indicator dots
 *   - Category selection cards (Capitals, Flags, Maps, Monuments) with circular progress indicators
 */
public class HomeFragment extends Fragment {
    // SharedPreferences storage keys
    private static final String PREFS                 = "geometric_prefs";
    private static final String KEY_XP                = "xp";
    private static final String KEY_STREAK            = "streak_days";
    private static final String KEY_STARS_CAPITALS     = "stars_capitals";
    private static final String KEY_STARS_FLAGS        = "stars_flags";
    private static final String KEY_STARS_MAPS         = "stars_maps";
    private static final String KEY_STARS_MONUMENTS    = "stars_monuments";

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate fragment layout
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        setupHeader(view);
        setupStreak(view);

        // Hide category cards before loading database statistics
        for (int id : new int[]{R.id.card_capitals, R.id.card_flags, R.id.card_maps, R.id.card_monuments}) {
            view.findViewById(id).setAlpha(0f);
        }

        setupCategoryCards(view);
    }

    // HEADER STATS
    /**
     * Reads saved user XP from SharedPreferences and displays it in the header text view.
     */
    private void setupHeader(View view) {
        int xp = prefs.getInt(KEY_XP, 0);
        TextView tvXp = view.findViewById(R.id.tv_xp);
        tvXp.setText(formatXp(xp));
    }

    /**
     * Formats XP numbers with spacing for readability (e.g., 1200 -> "1 200 XP").
     */
    private String formatXp(int xp) {
        if (xp >= 1000) {
            String s = String.valueOf(xp);
            return s.substring(0, s.length() - 3) + " " + s.substring(s.length() - 3) + " XP";
        }
        return xp + " XP";
    }

    // STREAK DOTS
    /**
     * Displays daily streak count and dynamically generates 10 streak indicator dots.
     */
    private void setupStreak(View view) {
        int streak = prefs.getInt(KEY_STREAK, 0);

        TextView tvLabel = view.findViewById(R.id.tv_streak_label);
        tvLabel.setText(streak + "-day streak");

        LinearLayout dotsContainer = view.findViewById(R.id.ll_streak_dots);
        dotsContainer.removeAllViews();

        int totalDots = 10;
        int dotSizePx = dpToPx(8);
        int dotMarginPx = dpToPx(4);

        // Generate filled and empty dot Views dynamically
        for (int i = 0; i < totalDots; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
            if (i > 0) lp.setMarginStart(dotMarginPx);
            dot.setLayoutParams(lp);
            dot.setBackground(ContextCompat.getDrawable(requireContext(),
                    i < streak ? R.drawable.streak_dot_filled : R.drawable.streak_dot_empty
            ));
            dotsContainer.addView(dot);
        }
    }

    // CATEGORY CARDS
    /**
     * Queries category progress from Room DB asynchronously and binds data to category cards.
     */
    private void setupCategoryCards(View view) {
        GeoMetricDatabase db = GeoMetricDatabase.getInstance(requireContext());

        String[] categories  = {"CAPITALS", "FLAGS", "MAPS", "MONUMENTS"};
        String[] titles      = {"Capitals", "Flags", "Maps", "Monuments"};
        int[]    cardIds     = {R.id.card_capitals, R.id.card_flags, R.id.card_maps, R.id.card_monuments};
        int[]    imageRes    = {R.drawable.city_berlin, R.drawable.flag_south_korea, R.drawable.map_brazil, R.drawable.mon_acropolis};
        int[]    colors      = {
                ContextCompat.getColor(requireContext(), R.color.accent_purple),
                ContextCompat.getColor(requireContext(), R.color.accent_red),
                ContextCompat.getColor(requireContext(), R.color.accent_green),
                ContextCompat.getColor(requireContext(), R.color.accent_orange)
        };
        String[] starKeys    = {KEY_STARS_CAPITALS, KEY_STARS_FLAGS, KEY_STARS_MAPS, KEY_STARS_MONUMENTS};

        // Query database on a single-thread background executor
        Executors.newSingleThreadExecutor().execute(() -> {
            
            // Wait for database initialization if this is the first run.
            // Wait until all 626 questions are fully inserted to avoid incomplete counts.
            while (db.questionDao().countAllQuestions() < 626) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            int[] passedBlocks = new int[4];
            int[] totalBlocks  = new int[4];
            int[] totalQuestions = new int[4];

            for (int i = 0; i < 4; i++) {
                List<BlockProgress> blocks =
                        db.blockProgressDao().getBlocksForCategory(categories[i]);
                totalBlocks[i] = blocks.size();
                for (BlockProgress bp : blocks) {
                    if (bp.bestScore >= QuizLogic.computePassThreshold(bp.questionCount)) {
                        passedBlocks[i]++;
                    }
                }
                totalQuestions[i] = db.questionDao().countQuestions(categories[i], 1)
                        + db.questionDao().countQuestions(categories[i], 2)
                        + db.questionDao().countQuestions(categories[i], 3);
            }

            if (getView() == null) return;

            // Switch back to UI thread to populate view elements
            requireActivity().runOnUiThread(() -> {
                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    int stars = prefs.getInt(starKeys[idx], 0);
                    bindCard(view,
                            cardIds[idx],
                            titles[idx],
                            totalQuestions[idx] + " questions",
                            imageRes[idx],
                            passedBlocks[idx],
                            totalBlocks[idx] > 0 ? totalBlocks[idx] : 1,
                            stars,
                            colors[idx],
                            v -> startCategory(categories[idx])
                    );
                }

                // Fade in category cards
                for (int id : new int[]{R.id.card_capitals, R.id.card_flags, R.id.card_maps, R.id.card_monuments}) {
                    view.findViewById(id).animate().alpha(1f).setDuration(200).start();
                }
            });
        });
    }

    /**
     * Helper method to populate visual elements of a category card.
     */
    private void bindCard(
            View rootView,
            int includeId,
            String title,
            String countLabel,
            int imageRes,
            int progress,
            int total,
            int stars,
            int accentColor,
            View.OnClickListener clickListener
    ) {
        View card = rootView.findViewById(includeId);
        if (card == null) return;

        ImageView iv = card.findViewById(R.id.iv_category_image);
        iv.setImageResource(imageRes);

        TextView tvName = card.findViewById(R.id.tv_category_name);
        tvName.setText(title);

        TextView tvCount = card.findViewById(R.id.tv_question_count);
        tvCount.setText(countLabel);

        CircularProgressIndicator ring = card.findViewById(R.id.progress_ring);
        ring.setIndicatorColor(accentColor);
        ring.setMax(total);
        ring.setProgress(progress, true);

        // Update star icons based on bitmask flags
        int[] starIds = { R.id.star_1, R.id.star_2, R.id.star_3 };
        for (int i = 0; i < starIds.length; i++) {
            ImageView star = card.findViewById(starIds[i]);
            boolean lit = (stars & (1 << i)) != 0;
            star.setImageResource(lit ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        }

        card.setOnClickListener(clickListener);
    }

    // NAVIGATION
    /**
     * Navigates to CategoryDetailFragment for the chosen category.
     */
    private void startCategory(String category) {
        CategoryDetailFragment next =
                CategoryDetailFragment.newInstance(category);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, next)
                .addToBackStack(null)
                .commit();
    }

    // HELPERS
    /**
     * Converts dp units to pixels based on current device screen density.
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Refresh header and category progress when fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            setupHeader(view);
            setupStreak(view);
            setupCategoryCards(view);
        }
    }
}
