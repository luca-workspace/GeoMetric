package com.luca.geometric;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Room Entity representing the progress and lock state of a single quiz block.
 *
 * Each block is uniquely identified by the combination of category, difficulty tier, and block index.
 * For example: ("CAPITALS", 1, 0) is the first block of Easy Capitals questions.
 *
 * Uses a composite primary key formed by (category, difficulty, blockIndex).
 */
@Entity(
        tableName = "block_progress_table",
        primaryKeys = {"category", "difficulty", "blockIndex"}
)
public class BlockProgress {

    @NonNull
    public String category;    // Category key: "CAPITALS", "FLAGS", "MAPS", "MONUMENTS"

    @NonNull
    public int difficulty;     // Difficulty tier: 1 (Easy), 2 (Medium), 3 (Hard)

    @NonNull
    public int blockIndex;     // 0-based block sequence index within (category, difficulty)

    /**
     * Flag indicating if the block can be selected and played by the user.
     * Block 0 of every category and difficulty is unlocked by default.
     * Subsequent blocks require passing the previous block with a score of at least 80%.
     */
    public boolean isUnlocked;

    /**
     * Best score achieved in this block (0 to questionCount).
     * Set to -1 if the block has not been played yet.
     */
    public int bestScore;

    /**
     * Number of times this block has been successfully passed with a score >= 80%.
     */
    public int completionCount;

    /**
     * Total number of questions contained in this block (typically 10).
     */
    public int questionCount;

    /**
     * Default empty constructor required by Room database framework.
     */
    public BlockProgress() {}

    /**
     * Constructor for creating a new BlockProgress instance during initial database population.
     */
    public BlockProgress(String category, int difficulty, int blockIndex, boolean isUnlocked, int questionCount) {
        this.category        = category;
        this.difficulty      = difficulty;
        this.blockIndex      = blockIndex;
        this.isUnlocked      = isUnlocked;
        this.bestScore       = -1; // Default -1 indicates unplayed state
        this.completionCount = 0;
        this.questionCount   = questionCount;
    }
}
