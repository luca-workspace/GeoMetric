package com.luca.geometric;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CategoryDetailFragment
 *
 * Displays the difficulty selection screen for a given category (Easy, Medium, Hard cards).
 * Each card shows a representative category image, the number of questions/blocks,
 * and a completion percentage calculated dynamically from the Room database.
 *
 * Arguments expected in Bundle:
 *   "category" String - e.g., "CAPITALS", "FLAGS", "MAPS", "MONUMENTS"
 */
public class CategoryDetailFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";

    // Representative image resource IDs organized by category and difficulty [categoryIndex][difficultyIndex]
    // Difficulty 0 (Easy), 1 (Medium), 2 (Hard)
    private static final int[][] CATEGORY_IMAGES = {
        // CAPITALS:  Easy = Paris, Medium = Nairobi, Hard = Ngerulmud
        { R.drawable.city_paris, R.drawable.city_nairobi, R.drawable.city_ngerulmud },
        // FLAGS:     Easy = France, Medium = Ghana, Hard = Kiribati
        { R.drawable.flag_france, R.drawable.flag_ghana, R.drawable.flag_kiribati },
        // MAPS:      Easy = France, Medium = Kenya, Hard = Palau
        { R.drawable.map_france, R.drawable.map_kenya, R.drawable.map_palau },
        // MONUMENTS: Easy = Eiffel Tower, Medium = Petra, Hard = Lalibela
        { R.drawable.mon_eiffel_tower, R.drawable.mon_petra, R.drawable.mon_lalibela },
    };

    // Array of category string keys matching the database format
    private static final String[] CATEGORY_KEYS =
        { "CAPITALS", "FLAGS", "MAPS", "MONUMENTS" };

    private String category;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Factory method to construct a CategoryDetailFragment instance with category argument.
     */
    public static CategoryDetailFragment newInstance(String category) {
        CategoryDetailFragment f = new CategoryDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve category argument
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY, "CAPITALS");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the XML layout for this fragment
        return inflater.inflate(R.layout.fragment_category_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupBackButton(view);
        setTitle(view);

        // Hide difficulty cards initially before database loading completes
        for (int id : new int[]{R.id.card_easy, R.id.card_medium, R.id.card_hard}) {
            view.findViewById(id).setAlpha(0f);
        }

        loadDifficultyCards(view);
    }

    /**
     * Applies top padding to header layout based on system status bar window insets.
     */
    private void applyStatusBarInset(View root) {
        View header = root.findViewById(R.id.ll_category_header);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dpToPx(4),
                         v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /**
     * Sets up click listener on the top back button to return to the previous screen.
     */
    private void setupBackButton(View root) {
        root.<ImageButton>findViewById(R.id.btn_category_back)
            .setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager().popBackStack());
    }

    /**
     * Sets formatted category name as header title (e.g., "Capitals").
     */
    private void setTitle(View root) {
        String label = category.charAt(0) + category.substring(1).toLowerCase();
        root.<TextView>findViewById(R.id.tv_category_title).setText(label);
    }

    /**
     * Loads progress and block counts for Easy, Medium, and Hard tiers in background thread.
     */
    private void loadDifficultyCards(View root) {
        GeoMetricDatabase db = GeoMetricDatabase.getInstance(requireContext());

        int[] cardViewIds = {
            R.id.card_easy, R.id.card_medium, R.id.card_hard
        };
        String[] labels   = { "Easy", "Medium", "Hard" };
        int[]    diffs    = { 1, 2, 3 };

        // Determine category index for selecting representative images
        int catIndex = 0;
        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            if (CATEGORY_KEYS[i].equals(category)) {
                catIndex = i;
                break;
            }
        }
        final int finalCatIndex = catIndex;

        // Perform Room queries on a background thread
        executor.execute(() -> {
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

            // results array stores: [difficultyIndex][0 = totalBlocks, 1 = passedBlocks, 2 = totalQuestions]
            int[][] results = new int[3][3];
            for (int i = 0; i < 3; i++) {
                List<BlockProgress> blocks = db.blockProgressDao()
                        .getBlocksForDifficulty(category, diffs[i]);
                int total  = blocks.size();
                int passed = 0;
                for (BlockProgress bp : blocks) {
                    if (bp.bestScore >= QuizLogic.computePassThreshold(bp.questionCount)) {
                        passed++;
                    }
                }
                results[i][0] = total;
                results[i][1] = passed;
                results[i][2] = db.questionDao().countQuestions(category, diffs[i]);
            }

            if (getView() == null) return;

            // Switch to UI thread to populate views and fade in cards
            requireActivity().runOnUiThread(() -> {
                for (int i = 0; i < 3; i++) {
                    View cardInclude = root.findViewById(cardViewIds[i]);
                    bindDifficultyCard(
                        cardInclude,
                        labels[i],
                        diffs[i],
                        CATEGORY_IMAGES[finalCatIndex][i],
                        results[i][0],
                        results[i][1],
                        results[i][2]
                    );
                }

                // Animate card visibility with alpha fade-in
                for (int id : new int[]{R.id.card_easy, R.id.card_medium, R.id.card_hard}) {
                    root.findViewById(id).animate().alpha(1f).setDuration(200).start();
                }
            });
        });
    }

    /**
     * Binds statistics, labels, and image resources to a single difficulty card view.
     */
    private void bindDifficultyCard(View card, String label, int difficulty,
                                    int imageRes, int totalBlocks, int passedBlocks, int questionCount) {
        // Set card image
        card.<ImageView>findViewById(R.id.iv_difficulty_image)
            .setImageResource(imageRes);

        // Set difficulty tier title (Easy / Medium / Hard)
        card.<TextView>findViewById(R.id.tv_difficulty_label).setText(label);

        // Set metadata text (e.g. "65 questions · 6 blocks")
        card.<TextView>findViewById(R.id.tv_difficulty_meta)
            .setText(questionCount + " questions · " + totalBlocks + " blocks");

        // Calculate and display progress percentage
        int pct = totalBlocks > 0 ? (passedBlocks * 100 / totalBlocks) : 0;
        card.<TextView>findViewById(R.id.tv_completion_pct)
            .setText(pct + "% complete");

        LinearProgressIndicator bar =
            card.findViewById(R.id.progress_difficulty);
        bar.setProgress(pct, true);

        // Clicking card navigates to block selection list for this difficulty
        ((MaterialCardView) card).setOnClickListener(v ->
            navigateToBlockList(label, difficulty));
    }

    /**
     * Opens BlockListFragment for the chosen category and difficulty tier.
     */
    private void navigateToBlockList(String difficultyLabel, int difficulty) {
        BlockListFragment next =
            BlockListFragment.newInstance(category, difficulty, difficultyLabel);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, next)
            .addToBackStack(null)
            .commit();
    }

    /**
     * Helper to convert density-independent pixels to screen pixels.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shut down background thread executor
        executor.shutdown();
    }
}
