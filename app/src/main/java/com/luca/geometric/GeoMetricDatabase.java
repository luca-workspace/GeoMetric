package com.luca.geometric;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Room Database class for the GeoMetric app.
 * Manages tables for Questions and BlockProgress records.
 * Uses the Singleton design pattern to prevent multiple database instances opening simultaneously.
 */
@Database(entities = {Question.class, BlockProgress.class}, version = 1)
public abstract class GeoMetricDatabase extends RoomDatabase {

    private static GeoMetricDatabase instance;

    // Abstract DAO getter methods implemented automatically by Room
    public abstract QuestionDao questionDao();
    public abstract BlockProgressDao blockProgressDao();

    // Single thread executor used for initial database population tasks
    private static final ExecutorService databaseWriteExecutor =
            Executors.newSingleThreadExecutor();

    private static final String[] CATEGORIES   = {"CAPITALS", "FLAGS", "MAPS", "MONUMENTS"};
    private static final int[]    DIFFICULTIES = {1, 2, 3};
    public  static final int      BLOCK_SIZE   = 10;

    /**
     * Singleton accessor method returning the database instance.
     */
    public static synchronized GeoMetricDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            GeoMetricDatabase.class,
                            "geometric_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Populate database asynchronously when database file is first created
                            databaseWriteExecutor.execute(() -> {
                                GeoMetricDatabase database = getInstance(context);
                                prePopulateDatabase(context, database.questionDao());
                                initBlockProgress(database);
                            });
                        }
                    })
                    .build();
        }
        return instance;
    }

    // JSON questions import
    /**
     * Reads questions from geo_qa.json asset file and inserts them into the database using QuestionDao.
     */
    private static void prePopulateDatabase(Context context, QuestionDao dao) {
        try {
            InputStream is = context.getAssets().open("geo_qa.json");
            InputStreamReader reader = new InputStreamReader(is);

            Gson gson = new Gson();
            Type type = new TypeToken<List<Question>>(){}.getType();
            List<Question> questions = gson.fromJson(reader, type);

            for (Question question : questions) {
                dao.insert(question);
            }

            reader.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Block progress initialisation

    /**
     * Creates initial BlockProgress records for all category and difficulty combinations.
     * Block 0 for each category/difficulty is unlocked by default, while subsequent blocks start locked.
     */
    public static void initBlockProgress(GeoMetricDatabase db) {
        QuestionDao      qDao = db.questionDao();
        BlockProgressDao bDao = db.blockProgressDao();

        List<BlockProgress> blocks = new ArrayList<>();

        for (String category : CATEGORIES) {
            for (int difficulty : DIFFICULTIES) {
                int total      = qDao.countQuestions(category, difficulty);
                int blockCount = total / BLOCK_SIZE;

                for (int i = 0; i < blockCount; i++) {
                    boolean unlocked = (i == 0); // First block is unlocked by default
                    int qCount = (i < blockCount - 1) ? BLOCK_SIZE : (total - i * BLOCK_SIZE);
                    blocks.add(new BlockProgress(category, difficulty, i, unlocked, qCount));
                }
            }
        }

        for (BlockProgress bp : blocks) {
            bDao.insert(bp);
        }
    }

    /**
     * Clears all progress records, resets wrong question counters, and re-initializes blocks.
     * Called when the user chooses "Reset Progress" in SettingsFragment.
     */
    public static void resetAndReinit(Context context) {
        GeoMetricDatabase db = getInstance(context);
        db.blockProgressDao().deleteAll();
        db.questionDao().resetAllWrongCounts();
        initBlockProgress(db);
    }
}
