package com.hz.appon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hz.appon.data.model.Category

/** Database access for trivia categories. */
@Dao
interface CategoryDao {

    /** Returns all categories, selected ones first. */
    @Query("SELECT * FROM categories ORDER BY isSelected DESC, name ASC")
    suspend fun getAll(): List<Category>

    /** Returns only categories the user has selected. */
    @Query("SELECT * FROM categories WHERE isSelected = 1")
    suspend fun getSelected(): List<Category>

    /** Inserts or replaces categories (used on first fetch from OpenTDB). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    /** Marks a single category as selected or deselected. */
    @Query("UPDATE categories SET isSelected = :selected WHERE id = :id")
    suspend fun setSelected(id: Int, selected: Boolean)

    /** Replaces the entire selection — deselects all, then selects given IDs. */
    @Query("UPDATE categories SET isSelected = 0")
    suspend fun clearAllSelections()
}
