package com.luca.geometric;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for interacting with the block_progress_table in Room database.
 * Provides SQL queries for creating, reading, updating block progress records and unlocking blocks.
 */
@Dao
public interface BlockProgressDao {

    /**
     * Inserts a new block progress record into the table.
     * Ignores insertion if a record with the same composite key already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(BlockProgress block);

    /**
     * Inserts or replaces a block progress record. Used during backup import.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(BlockProgress block);

    /**
     * Updates an existing block progress record with new score or unlocked status.
     */
    @Update
    void update(BlockProgress block);

    /**
     * Retrieves all blocks belonging to a given category, sorted by difficulty and block index.
     */
    @Query("SELECT * FROM block_progress_table " +
            "WHERE category = :category " +
            "ORDER BY difficulty ASC, blockIndex ASC")
    List<BlockProgress> getBlocksForCategory(String category);

    /**
     * Retrieves all blocks for a specific difficulty tier in a given category.
     */
    @Query("SELECT * FROM block_progress_table " +
            "WHERE category = :category AND difficulty = :difficulty " +
            "ORDER BY blockIndex ASC")
    List<BlockProgress> getBlocksForDifficulty(String category, int difficulty);

    /**
     * Retrieves a single block record matching the specific category, difficulty, and block index.
     * Used when updating block score and completion status after finishing a quiz session.
     */
    @Query("SELECT * FROM block_progress_table " +
            "WHERE category = :category AND difficulty = :difficulty AND blockIndex = :blockIndex " +
            "LIMIT 1")
    BlockProgress getBlock(String category, int difficulty, int blockIndex);

    /**
     * Updates the isUnlocked flag to true for a specific block.
     * Called when a user passes the preceding block in the sequence.
     */
    @Query("UPDATE block_progress_table SET isUnlocked = 1 " +
            "WHERE category = :category AND difficulty = :difficulty AND blockIndex = :blockIndex")
    void unlockBlock(String category, int difficulty, int blockIndex);

    /**
     * Retrieves all block progress records for backup.
     */
    @Query("SELECT * FROM block_progress_table")
    List<BlockProgress> getAllBlocks();

    /**
     * Deletes all records from the block progress table.
     * Used when resetting user progress in SettingsFragment.
     */
    @Query("DELETE FROM block_progress_table")
    void deleteAll();
}
