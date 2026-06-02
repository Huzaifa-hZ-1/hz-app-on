package com.hz.appon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A trivia category fetched from OpenTDB and persisted locally.
 *
 * @param id OpenTDB numeric category ID (9–32)
 * @param name Human-readable category name
 * @param isSelected Whether the user has selected this category during onboarding
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: Int,
    val name: String,
    val isSelected: Boolean = false
)
