package com.hz.appon.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.hz.appon.data.model.Difficulty

/**
 * Room TypeConverters for non-primitive types.
 * Room cannot store List<String> or enums natively — these converters handle serialisation.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, Array<String>::class.java).toList()

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = Difficulty.valueOf(value)
}
