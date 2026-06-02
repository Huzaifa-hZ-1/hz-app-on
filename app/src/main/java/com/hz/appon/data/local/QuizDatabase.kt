package com.hz.appon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Question

/**
 * Single Room database for the app.
 *
 * Increment [version] and provide a Migration whenever the schema changes.
 * Never use `fallbackToDestructiveMigration` in production.
 */
@Database(entities = [Category::class, Question::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile private var INSTANCE: QuizDatabase? = null

        /** Returns the singleton database instance, creating it if necessary. */
        fun getInstance(context: Context): QuizDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_database"
                ).build().also { INSTANCE = it }
            }
    }
}
