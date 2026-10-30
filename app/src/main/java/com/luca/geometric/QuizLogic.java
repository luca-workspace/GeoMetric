package com.luca.geometric;

/**
 * Helper class containing pure logic and calculations for the quiz app.
 * Keeping these functions separate from Android UI classes makes them easy to test.
 */
public class QuizLogic {

    /**
     * Calculates the updated daily streak based on the current streak count,
     * the date of the user's last session, and today's date (formatted as "YYYY-MM-DD").
     */
    public static int computeStreak(int currentStreak, String lastPlayedDate, String today) {
        if (today.equals(lastPlayedDate)) {
            return currentStreak; // Already played today, streak remains the same
        }
        String yesterday = getPreviousDate(today);
        if (yesterday.equals(lastPlayedDate)) {
            return currentStreak + 1; // Played yesterday, increment streak
        }
        return 1; // Missed one or more days, reset streak to 1
    }

    /**
     * Calculates the minimum score needed to pass a block (80% rounded up).
     */
    public static int computePassThreshold(int questionCount) {
        return (int) Math.ceil(questionCount * 0.8);
    }

    /**
     * Calculates the SQL database offset for retrieving questions for a specific block.
     */
    public static int computeOffset(int blockIndex, int blockSize) {
        return blockIndex * blockSize;
    }

    /**
     * Calculates the total number of blocks based on question count and block size.
     */
    public static int computeBlockCount(int totalQuestions, int blockSize) {
        return (totalQuestions + blockSize - 1) / blockSize;
    }

    /**
     * Calculates the number of questions in the last block (which might be smaller than full block size).
     */
    public static int computeLastBlockSize(int totalQuestions, int blockSize) {
        int remainder = totalQuestions % blockSize;
        return remainder == 0 ? blockSize : remainder;
    }

    /**
     * Returns the previous day's date string in "YYYY-MM-DD" format.
     * Written without complex date libraries for simple unit testing.
     */
    static String getPreviousDate(String date) {
        // Manual parsing of year, month, and day from the "YYYY-MM-DD" string
        int year  = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day   = Integer.parseInt(date.substring(8, 10));

        day--;
        if (day == 0) {
            month--;
            if (month == 0) {
                month = 12;
                year--;
            }
            day = daysInMonth(month, year);
        }
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    /**
     * Returns the number of days in a given month, accounting for leap years.
     */
    static int daysInMonth(int month, int year) {
        switch (month) {
            case 2:
                // Check if the year is a leap year
                return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            case 4: case 6: case 9: case 11:
                return 30;
            default:
                return 31;
        }
    }
}
