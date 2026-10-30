package com.luca.geometric;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a single quiz question in the database table questions_table.
 * Stores question body, option choices, correct index, image drawable name, hint text,
 * and wrong answer count for spaced repetition.
 */
@Entity(tableName = "questions_table")
public class Question {
    @PrimaryKey
    @ColumnInfo(name = "id")
    private int id; // Unique question ID in the database

    @ColumnInfo(name = "category")
    private String category;       // Question category: "CAPITALS", "FLAGS", "MAPS", "MONUMENTS"

    @ColumnInfo(name = "difficulty")
    private int difficulty;        // Difficulty level: 1 (Easy), 2 (Medium), 3 (Hard)

    @ColumnInfo(name = "text")
    private String text;           // The main question prompt text

    // The four option choices for multiple-choice answers
    @ColumnInfo(name = "optionA")
    private String optionA;

    @ColumnInfo(name = "optionB")
    private String optionB;

    @ColumnInfo(name = "optionC")
    private String optionC;

    @ColumnInfo(name = "optionD")
    private String optionD;

    @ColumnInfo(name = "correctIndex")
    private int correctIndex;      // Index of correct option: 0 = A, 1 = B, 2 = C, 3 = D

    @ColumnInfo(name = "imageName")
    private String imageName;      // Drawable resource name (e.g., "flag_italy"), or empty if none

    @ColumnInfo(name = "hint")
    private String hint;           // Hint text displayed when user taps hint button

    /**
     * Counter tracking how many times the user answered this question incorrectly.
     * Used by Room queries to prioritize wrong questions at the beginning of quiz blocks.
     * Defaults to 0 for new or correctly answered questions.
     */
    @ColumnInfo(name = "wrongCount")
    private int wrongCount = 0;

    /**
     * Default constructor required by Room.
     */
    public Question() {}

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }

    /**
     * Helper method to return answer option text by index (0 for A, 1 for B, 2 for C, 3 for D).
     *
     * @param index Option index (0 to 3)
     * @return Answer text string
     */
    public String getOptionByIndex(int index) {
        switch (index) {
            case 0: return optionA;
            case 1: return optionB;
            case 2: return optionC;
            case 3: return optionD;
            default: return "";
        }
    }
}
