package com.hz.appon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question

/** Database access for trivia questions. */
@Dao
interface QuestionDao {

    /**
     * Returns up to [limit] cached questions for a category and difficulty.
     * Results are randomised so repeated calls return different question sets.
     */
    @Query(
        "SELECT * FROM questions WHERE categoryId = :categoryId " +
        "AND difficulty = :difficulty AND isBundled = 0 " +
        "ORDER BY RANDOM() LIMIT :limit"
    )
    suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, limit: Int): List<Question>

    /** Returns all 5 bundled offline questions. */
    @Query("SELECT * FROM questions WHERE isBundled = 1")
    suspend fun getBundled(): List<Question>

    /** Inserts or replaces questions (used after fetching from OpenTDB). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    /** Returns the number of cached questions for a category/difficulty. */
    @Query(
        "SELECT COUNT(*) FROM questions WHERE categoryId = :categoryId " +
        "AND difficulty = :difficulty AND isBundled = 0"
    )
    suspend fun countQuestions(categoryId: Int, difficulty: Difficulty): Int
}
