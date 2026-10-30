package com.luca.geometric;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BlockListFragment
 *
 * Displays a list of quiz blocks for a specific category and difficulty combination.
 * Each block card shows its block number, current best score progress bar, and locked/unlocked state.
 * Clicking an unlocked block launches QuizActivity to start the quiz session.
 *
 * Arguments expected in Bundle:
 *   "category"        String  - e.g., "CAPITALS", "FLAGS", "MAPS", "MONUMENTS"
 *   "difficulty"      int     - 1 (Easy), 2 (Medium), or 3 (Hard)
 *   "difficultyLabel" String  - "Easy", "Medium", or "Hard" (used for header title)
 */
public class BlockListFragment extends Fragment {

    // Argument key constants used when passing data into this fragment
    private static final String ARG_CATEGORY         = "category";
    private static final String ARG_DIFFICULTY       = "difficulty";
    private static final String ARG_DIFFICULTY_LABEL = "difficultyLabel";

    // Member variables to store the current category and difficulty configuration
    private String category;
    private int    difficulty;
    private String difficultyLabel;

    // Single-thread executor for running database queries off the main UI thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Factory method to create a new instance of BlockListFragment with arguments.
     */
    public static BlockListFragment newInstance(String category,
                                                int difficulty,
                                                String difficultyLabel) {
        BlockListFragment f = new BlockListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putInt(ARG_DIFFICULTY, difficulty);
        args.putString(ARG_DIFFICULTY_LABEL, difficultyLabel);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Extract arguments passed from the calling fragment
        if (getArguments() != null) {
            category        = getArguments().getString(ARG_CATEGORY, "CAPITALS");
            difficulty      = getArguments().getInt(ARG_DIFFICULTY, 1);
            difficultyLabel = getArguments().getString(ARG_DIFFICULTY_LABEL, "Easy");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the XML layout for this fragment
        return inflater.inflate(R.layout.fragment_block_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components and start loading data
        setupBackButton(view);
        setTitle(view);
        loadBlocks(view);
    }

    /**
     * Adjusts top padding of the header layout to prevent overlap with the system status bar.
     */
    private void applyStatusBarInset(View root) {
        View header = root.findViewById(R.id.ll_blocklist_header);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dpToPx(4),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /**
     * Sets up the top back button to pop the current fragment off the back stack.
     */
    private void setupBackButton(View root) {
        root.<ImageButton>findViewById(R.id.btn_blocklist_back)
                .setOnClickListener(v -> requireActivity()
                        .getSupportFragmentManager().popBackStack());
    }

    /**
     * Sets the header title by formatting category name and difficulty (e.g., "Capitals - Easy").
     */
    private void setTitle(View root) {
        String catLabel = category.charAt(0) + category.substring(1).toLowerCase();
        root.<TextView>findViewById(R.id.tv_blocklist_title)
                .setText(catLabel + " — " + difficultyLabel);
    }

    /**
     * Queries the Room database in a background thread to load all blocks for this difficulty.
     * Computes overall pass statistics and updates the RecyclerView on the UI thread.
     */
    private void loadBlocks(View root) {
        GeoMetricDatabase db = GeoMetricDatabase.getInstance(requireContext());

        executor.execute(() -> {
            // Load block progress records from database
            List<BlockProgress> blocks = db.blockProgressDao()
                    .getBlocksForDifficulty(category, difficulty);

            // Calculate overall passed blocks count for the header summary
            int passed = 0;
            for (BlockProgress bp : blocks) {
                if (bp.bestScore >= QuizLogic.computePassThreshold(bp.questionCount)) {
                    passed++;
                }
            }
            final int finalPassed = passed;
            final int total       = blocks.size();

            if (getView() == null) return;

            // Switch back to the main UI thread to update views
            requireActivity().runOnUiThread(() -> {
                // Display overall progress percentage and passed count
                int pct = total > 0 ? (finalPassed * 100 / total) : 0;
                root.<TextView>findViewById(R.id.tv_overall_progress)
                        .setText(finalPassed + " of " + total + " blocks passed");
                root.<TextView>findViewById(R.id.tv_overall_pct)
                        .setText(pct + "%");

                // Configure RecyclerView with LinearAdapter and set the block adapter
                RecyclerView rv = root.findViewById(R.id.rv_blocks);
                rv.setLayoutManager(new LinearLayoutManager(requireContext()));
                rv.setAdapter(new BlockAdapter(blocks, this::onBlockTapped));
            });
        });
    }

    /**
     * Called when a user clicks on a block item in the RecyclerView list.
     * If unlocked, starts QuizActivity with category, difficulty, and block index intent extras.
     */
    private void onBlockTapped(BlockProgress bp) {
        // Do nothing if the block is locked
        if (!bp.isUnlocked) return;

        // Launch the quiz activity for the selected block if the block is unlocked
        Intent intent = new Intent(requireContext(), QuizActivity.class);
        intent.putExtra(QuizActivity.EXTRA_CATEGORY,   bp.category);
        intent.putExtra(QuizActivity.EXTRA_DIFFICULTY, bp.difficulty);
        intent.putExtra(QuizActivity.EXTRA_BLOCK_INDEX, bp.blockIndex);
        startActivity(intent);
    }

    /**
     * Helper method to convert density-independent pixels (dp) to screen pixels (px).
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shut down executor thread pool when fragment is destroyed
        executor.shutdown();
    }

    /**
     * Refresh block list when returning to this fragment, as scores may have updated during quiz.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadBlocks(getView());
        }
    }
}
