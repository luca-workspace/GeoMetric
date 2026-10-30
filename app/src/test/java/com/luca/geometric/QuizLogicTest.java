package com.luca.geometric;

import org.junit.Test;
import static org.junit.Assert.*;

public class QuizLogicTest {

    // computeStreak

    @Test
    public void streak_sameDay_unchanged() {
        assertEquals(5, QuizLogic.computeStreak(5, "2026-05-17", "2026-05-17"));
    }

    @Test
    public void streak_consecutiveDay_increments() {
        assertEquals(6, QuizLogic.computeStreak(5, "2026-05-16", "2026-05-17"));
    }

    @Test
    public void streak_gap_resetsToOne() {
        assertEquals(1, QuizLogic.computeStreak(5, "2026-05-10", "2026-05-17"));
    }

    @Test
    public void streak_neverPlayed_startsAtOne() {
        assertEquals(1, QuizLogic.computeStreak(0, "", "2026-05-17"));
    }

    @Test
    public void streak_acrossMonthBoundary() {
        // May 1st is consecutive to April 30th
        assertEquals(4, QuizLogic.computeStreak(3, "2026-04-30", "2026-05-01"));
    }

    @Test
    public void streak_acrossYearBoundary() {
        assertEquals(2, QuizLogic.computeStreak(1, "2025-12-31", "2026-01-01"));
    }

    // computePassThreshold

    @Test
    public void threshold_fullBlock_is8() {
        assertEquals(8, QuizLogic.computePassThreshold(10));
    }

    @Test
    public void threshold_partialBlock_roundsUp() {
        // 80% of 7 = 5.6 → ceil = 6
        assertEquals(6, QuizLogic.computePassThreshold(7));
    }

    @Test
    public void threshold_twoQuestions_is2() {
        // 80% of 2 = 1.6 → ceil = 2
        assertEquals(2, QuizLogic.computePassThreshold(2));
    }

    // computeBlockCount

    @Test
    public void blockCount_exact_noRemainder() {
        assertEquals(5, QuizLogic.computeBlockCount(50, 10));
    }

    @Test
    public void blockCount_withRemainder_roundsUp() {
        assertEquals(6, QuizLogic.computeBlockCount(52, 10));
    }

    @Test
    public void blockCount_lessThanBlockSize_isOne() {
        assertEquals(1, QuizLogic.computeBlockCount(7, 10));
    }

    // computeLastBlockSize

    @Test
    public void lastBlockSize_exact_equalsBlockSize() {
        assertEquals(10, QuizLogic.computeLastBlockSize(50, 10));
    }

    @Test
    public void lastBlockSize_withRemainder_returnsRemainder() {
        assertEquals(2, QuizLogic.computeLastBlockSize(52, 10));
    }

    // computeOffset

    @Test
    public void offset_firstBlock_isZero() {
        assertEquals(0, QuizLogic.computeOffset(0, 10));
    }

    @Test
    public void offset_thirdBlock_is20() {
        assertEquals(20, QuizLogic.computeOffset(2, 10));
    }

    // getPreviousDate

    @Test
    public void previousDate_normalDay() {
        assertEquals("2026-05-16", QuizLogic.getPreviousDate("2026-05-17"));
    }

    @Test
    public void previousDate_firstOfMonth() {
        assertEquals("2026-04-30", QuizLogic.getPreviousDate("2026-05-01"));
    }

    @Test
    public void previousDate_firstOfYear() {
        assertEquals("2025-12-31", QuizLogic.getPreviousDate("2026-01-01"));
    }

    @Test
    public void previousDate_leapYear() {
        assertEquals("2024-02-29", QuizLogic.getPreviousDate("2024-03-01"));
    }
}