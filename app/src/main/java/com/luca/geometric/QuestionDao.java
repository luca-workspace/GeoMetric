package com.luca.geometric;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for questions_table operations in Room database.
 */
@Dao
public interface QuestionDao {

    /**
     * Inserts a question record into the table, replacing any existing record on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Question question);

    /**
     * Inserts a list of questions into the table.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Question> questions);

    /**
     * Updates an existing question record (e.g., updating wrongCount).
     */
    @Update
    void updateQuestion(Question question);

    /**
     * Retrieves questions for a specific quiz block based on category, difficulty, limit, and offset.
     *
     * Sorting: "ORDER BY wrongCount DESC, id ASC" ensures questions the user previously missed
     * appear at the beginning of the block to reinforce learning, followed by stable ID ordering.
     */
    @Query("SELECT * FROM questions_table " +
            "WHERE category = :category AND difficulty = :difficulty " +
            "ORDER BY wrongCount DESC, id ASC " +
            "LIMIT :limit OFFSET :offset")
    List<Question> getBlockQuestions(String category, int difficulty, int limit, int offset);

    /**
     * Counts total questions for a category and difficulty level.
     * Used when dividing questions into 10-question blocks during initialization.
     */
    @Query("SELECT COUNT(*) FROM questions_table " +
            "WHERE category = :category AND difficulty = :difficulty")
    int countQuestions(String category, int difficulty);

    /**
     * Counts total questions across all categories and difficulties.
     * Used to wait for full database initialization.
     */
    @Query("SELECT COUNT(*) FROM questions_table")
    int countAllQuestions();

    /**
     * Resets wrongCount back to 0 for all questions in the database.
     * Called when the user resets app progress in SettingsFragment.
     */
    @Query("UPDATE questions_table SET wrongCount = 0")
    void resetAllWrongCounts();

    /**
     * Retrieves all questions that have a wrongCount > 0 for backup purposes.
     */
    @Query("SELECT * FROM questions_table WHERE wrongCount > 0")
    List<Question> getQuestionsWithWrongCounts();

    /**
     * Updates the wrongCount for a specific question.
     */
    @Query("UPDATE questions_table SET wrongCount = :wrongCount WHERE id = :id")
    void updateWrongCount(int id, int wrongCount);

    /**
     * Helper query returning random questions for a category and difficulty.
     */
    @Query("SELECT * FROM questions_table WHERE category = :cat AND difficulty = :diff ORDER BY RANDOM() LIMIT 5")
    List<Question> getRandomQuiz(String cat, int diff);
}
