package com.luca.geometric;

import android.content.SharedPreferences;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * QuizActivity
 *
 * Activity responsible for running an interactive quiz session for a selected block.
 *
 * Workflow:
 *   1. Fetch questions from Room DB asynchronously (prioritizing previously missed questions).
 *   2. Display question prompt, optional image, hint button, and four answer option cards.
 *   3. On option tap: highlight correct (green) and wrong (red) choices, trigger sound/vibration,
 *      and update wrong answer count in database if incorrect.
 *   4. Advance to next question after a short delay.
 *   5. On completing the block: evaluate score against passing threshold (80%), unlock next block,
 *      update SharedPreferences (XP, daily streak, category star badges), and present result dialog.
 */
public class QuizActivity extends AppCompatActivity {
    // Intent Extra Keys
    public static final String EXTRA_CATEGORY    = "category";
    public static final String EXTRA_DIFFICULTY  = "difficulty";
    public static final String EXTRA_BLOCK_INDEX = "blockIndex";

    // Delay duration (ms) before advancing to the next question after an answer tap
    private static final int NEXT_QUESTION_DELAY_MS = 1200;

    // SharedPreferences Storage Keys
    private static final String KEY_XP               = "xp";
    private static final String KEY_STREAK           = "streak_days";
    private static final String KEY_LAST_PLAYED_DATE = "last_played_date";
    private static final String KEY_STARS_CAPITALS   = "stars_capitals";
    private static final String KEY_STARS_FLAGS      = "stars_flags";
    private static final String KEY_STARS_MAPS       = "stars_maps";
    private static final String KEY_STARS_MONUMENTS  = "stars_monuments";

    // Session State Variables
    private String          category;
    private int             difficulty;
    private int             blockIndex;
    private List<Question>  questions;
    private int             currentIndex = 0;
    private int             score        = 0;
    private boolean         answered     = false; // Prevents double-tap issues on option cards

    // Database & Executor
    private GeoMetricDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Components
    private LinearProgressIndicator progressBar;
    private TextView                tvCounter;
    private ImageView               ivQuestionImage;
    private TextView                tvQuestionText;
    private View                    btnHint;
    private TextView                tvHint;

    // Arrays holding references to the 4 multiple choice option cards and inner views
    private MaterialCardView[] optionCards   = new MaterialCardView[4];
    private TextView[]         optionLetters = new TextView[4];
    private TextView[]         optionTexts   = new TextView[4];
    private ImageView[]        optionIcons   = new ImageView[4];

    // Sound effects
    private SoundPool soundPool;
    private int soundCorrect, soundWrong, soundQuizComplete, soundQuizFailed;

    // Preferences
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        prefs = getSharedPreferences(GeoMetricApplication.PREFS, MODE_PRIVATE);
        db = GeoMetricDatabase.getInstance(this);

        // Retrieve quiz settings passed via Intent extras
        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        difficulty = getIntent().getIntExtra(EXTRA_DIFFICULTY, 1);
        blockIndex = getIntent().getIntExtra(EXTRA_BLOCK_INDEX, 0);

        bindViews();

        // Initialize SoundPool for playing audio feedback
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .build();
        soundCorrect      = soundPool.load(this, R.raw.sound_correct, 1);
        soundWrong        = soundPool.load(this, R.raw.sound_wrong, 1);
        soundQuizComplete = soundPool.load(this, R.raw.sound_quiz_complete, 1);
        soundQuizFailed   = soundPool.load(this, R.raw.sound_quiz_failed, 1);

        applyStatusBarInset();
        setupBackButton();
        loadQuestions();
    }

    // View Binding
    /**
     * Binds UI component references from activity_quiz layout XML.
     */
    private void bindViews() {
        progressBar     = findViewById(R.id.progress_quiz);
        tvCounter       = findViewById(R.id.tv_quiz_counter);
        ivQuestionImage = findViewById(R.id.iv_question_image);
        tvQuestionText  = findViewById(R.id.tv_question_text);
        btnHint         = findViewById(R.id.btn_hint);
        tvHint          = findViewById(R.id.tv_hint);

        int[] cardIds = {R.id.option_a, R.id.option_b, R.id.option_c, R.id.option_d};
        String[] labels = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            View root        = findViewById(cardIds[i]);
            optionCards[i]   = (MaterialCardView) root;
            optionLetters[i] = root.findViewById(R.id.tv_option_letter);
            optionTexts[i]   = root.findViewById(R.id.tv_option_text);
            optionIcons[i]   = root.findViewById(R.id.iv_option_result);
            optionLetters[i].setText(labels[i]);

            final int index = i;
            optionCards[i].setOnClickListener(v -> onOptionSelected(index));
        }
    }

    /**
     * Applies WindowInsets padding to the header to prevent overlap with status bar.
     */
    private void applyStatusBarInset() {
        View header = findViewById(R.id.ll_quiz_header);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dpToPx(4),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /**
     * Configures the header back button to exit the quiz activity.
     */
    private void setupBackButton() {
        ImageButton btn = findViewById(R.id.btn_quiz_back);
        btn.setOnClickListener(v -> finish());
    }

    // Load Questions
    /**
     * Loads block questions asynchronously from Room database.
     */
    private void loadQuestions() {
        int offset = blockIndex * GeoMetricDatabase.BLOCK_SIZE;
        executor.execute(() -> {
            BlockProgress bp = db.blockProgressDao().getBlock(category, difficulty, blockIndex);
            int limit = (bp != null && bp.questionCount > 0) ? bp.questionCount : GeoMetricDatabase.BLOCK_SIZE;
            questions = db.questionDao().getBlockQuestions(category, difficulty, limit, offset);

            runOnUiThread(() -> {
                if (questions == null || questions.isEmpty()) {
                    finish();
                    return;
                }
                progressBar.setMax(questions.size());
                showQuestion(0);
            });
        });
    }

    // Display Question
    /**
     * Populates views with question prompt, image resource, hint, and option text choices.
     */
    private void showQuestion(int index) {
        answered     = false;
        currentIndex = index;

        Question q = questions.get(index);

        // Update progress bar and question counter
        progressBar.setProgress(index);
        tvCounter.setText((index + 1) + " / " + questions.size());

        // Load image if present
        if (q.getImageName() != null && !q.getImageName().isEmpty()) {
            int resId = getResources().getIdentifier(
                    q.getImageName(), "drawable", getPackageName());
            if (resId != 0) {
                ivQuestionImage.setImageResource(resId);
                ivQuestionImage.setVisibility(View.VISIBLE);
            } else {
                ivQuestionImage.setVisibility(View.GONE);
            }
        } else {
            ivQuestionImage.setVisibility(View.GONE);
        }

        // Set question text
        tvQuestionText.setText(q.getText());

        // Configure hint view button
        if (q.getHint() != null && !q.getHint().isEmpty()) {
            btnHint.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.GONE);
            tvHint.setText(q.getHint());
            btnHint.setOnClickListener(v -> {
                btnHint.setVisibility(View.GONE);
                tvHint.setVisibility(View.VISIBLE);
            });
        } else {
            btnHint.setVisibility(View.GONE);
            tvHint.setVisibility(View.GONE);
        }

        // Reset option card visual appearance for new question
        for (int i = 0; i < 4; i++) {
            optionCards[i].setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.surface));
            optionCards[i].setStrokeColor(
                    ContextCompat.getColor(this, R.color.border));
            optionTexts[i].setTextColor(
                    ContextCompat.getColor(this, R.color.text_primary));
            optionIcons[i].setVisibility(View.GONE);
            optionCards[i].setEnabled(true);
        }

        optionTexts[0].setText(q.getOptionA());
        optionTexts[1].setText(q.getOptionB());
        optionTexts[2].setText(q.getOptionC());
        optionTexts[3].setText(q.getOptionD());
    }

    // Handle Answer Selection
    /**
     * Handles user tap on an option card. Validates answer, updates UI colors, plays sounds/haptics,
     * updates database records, and schedules advance to next question.
     */
    private void onOptionSelected(int selectedIndex) {
        if (answered) return; // Prevent multiple taps
        answered = true;

        // Disable cards to prevent further clicks
        for (MaterialCardView card : optionCards) card.setEnabled(false);

        Question q       = questions.get(currentIndex);
        boolean  correct = (selectedIndex == q.getCorrectIndex());

        if (correct) {
            score++;
            colorOption(selectedIndex, true);
            vibrate(50);
            playSound(soundCorrect);
        } else {
            colorOption(selectedIndex, false);       // Highlight selected option as incorrect (red)
            colorOption(q.getCorrectIndex(), true);  // Reveal correct option (green)
            vibrate(300);
            playSound(soundWrong);
            incrementWrongCount(q);                  // Increment wrong answer counter in DB
        }

        // Advance progress bar and schedule next question transition
        progressBar.setProgress(currentIndex + 1);
        optionCards[0].postDelayed(() -> {
            if (currentIndex + 1 < questions.size()) {
                showQuestion(currentIndex + 1);
            } else {
                onBlockFinished();
            }
        }, NEXT_QUESTION_DELAY_MS);
    }

    /**
     * Applies correct (green) or wrong (red) background, border stroke, and icon to an option card.
     */
    private void colorOption(int index, boolean correct) {
        int bgColor     = ContextCompat.getColor(this,
                correct ? R.color.answer_correct_bg : R.color.answer_wrong_bg);
        int strokeColor = ContextCompat.getColor(this,
                correct ? R.color.answer_correct : R.color.answer_wrong);
        int iconRes     = correct ? R.drawable.ic_check : R.drawable.ic_close;

        optionCards[index].setCardBackgroundColor(bgColor);
        optionCards[index].setStrokeColor(strokeColor);
        optionIcons[index].setImageResource(iconRes);
        optionIcons[index].setImageTintList(
                ContextCompat.getColorStateList(this, correct
                        ? R.color.answer_correct : R.color.answer_wrong));
        optionIcons[index].setVisibility(View.VISIBLE);
    }

    /**
     * Increments wrongCount for a question in Room DB on background thread.
     */
    private void incrementWrongCount(Question q) {
        executor.execute(() -> {
            q.setWrongCount(q.getWrongCount() + 1);
            db.questionDao().updateQuestion(q);
        });
    }

    // Block Completion Processing
    /**
     * Called when all questions in the block are completed.
     * Evaluates score against 80% passing threshold, updates block progress, unlocks next block,
     * updates user XP and daily streak in SharedPreferences, and displays result dialog.
     */
    private void onBlockFinished() {
        int threshold = QuizLogic.computePassThreshold(questions.size());
        boolean passed = score >= threshold;

        executor.execute(() -> {
            BlockProgressDao bDao = db.blockProgressDao();

            // Update block progress record
            BlockProgress bp = bDao.getBlock(category, difficulty, blockIndex);
            if (bp != null) {
                if (score > bp.bestScore) bp.bestScore = score;
                if (passed) {
                    bp.completionCount++;
                    // Unlock next block in sequence
                    bDao.unlockBlock(category, difficulty, blockIndex + 1);
                }
                bDao.update(bp);
            }

            // Award +10 XP per correct answer
            int earned = score * 10;
            int newXp  = prefs.getInt("xp", 0) + earned;

            // Update daily streak tracking
            String todayStr = LocalDate.now().toString();
            String lastPlayed = prefs.getString(KEY_LAST_PLAYED_DATE, "");
            int streak = prefs.getInt(KEY_STREAK, 0);

            if (!todayStr.equals(lastPlayed)) {
                streak = QuizLogic.computeStreak(streak, lastPlayed, todayStr);
            }

            // Calculate updated star badge mask for category
            int stars = computeStars(category);
            String starsKey = starsKeyFor(category);

            // Commit SharedPreferences updates
            prefs.edit()
                    .putInt(KEY_XP, newXp)
                    .putInt(KEY_STREAK, streak)
                    .putString(KEY_LAST_PLAYED_DATE, todayStr)
                    .putInt(starsKey, stars)
                    .apply();

            final int finalStreak = streak;
            runOnUiThread(() -> showResultOverlay(passed, score, earned));
        });
    }

    /**
     * Calculates star bitmask for category completion across difficulties.
     */
    private int computeStars(String cat) {
        BlockProgressDao bDao = db.blockProgressDao();
        int bitmask = 0;

        for (int diff = 1; diff <= 3; diff++) {
            List<BlockProgress> blocks = bDao.getBlocksForDifficulty(cat, diff);
            if (blocks.isEmpty()) continue;
            int passed = 0;
            for (BlockProgress b : blocks) {
                if (b.bestScore >= QuizLogic.computePassThreshold(b.questionCount)) passed++;
            }
            if (passed == blocks.size()) {
                bitmask |= (1 << (diff - 1)); // bit 0 = Easy, bit 1 = Medium, bit 2 = Hard
            }
        }
        return bitmask;
    }

    private String starsKeyFor(String cat) {
        switch (cat) {
            case "FLAGS":     return KEY_STARS_FLAGS;
            case "MAPS":      return KEY_STARS_MAPS;
            case "MONUMENTS": return KEY_STARS_MONUMENTS;
            default:          return KEY_STARS_CAPITALS;
        }
    }

    // Result Dialog Overlay
    /**
     * Displays result AlertDialog showing score, earned XP, and pass/fail message.
     */
    private void showResultOverlay(boolean passed, int score, int xpEarned) {
        String title   = passed ? "Well done! 🎉" : "Keep practising";
        String message = score + " / " + questions.size() + " correct\n+"
                + xpEarned + " XP earned";

        if (!passed) {
            message += "\n\nYou need to get at least 80% of answers right to unlock the next block.";
            playSound(soundQuizFailed);
        } else {
            playSound(soundQuizComplete);
        }

        new androidx.appcompat.app.AlertDialog.Builder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Continue", (d, w) -> finish())
                .show();
    }

    /**
     * Plays sound effect if sounds setting is enabled in preferences.
     */
    private void playSound(int soundId) {
        if (prefs.getBoolean("sounds_enabled", true)) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f);
        }
    }

    /**
     * Triggers device vibration feedback if vibration setting is enabled.
     */
    private void vibrate(long ms) {
        if (!prefs.getBoolean("vibration_enabled", true)) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vm.getDefaultVibrator().vibrate(
                        VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(ms);
                }
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down background thread executor
        executor.shutdown();

        // Release SoundPool resources
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
