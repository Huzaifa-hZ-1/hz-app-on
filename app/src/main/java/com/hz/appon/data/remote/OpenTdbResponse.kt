package com.hz.appon.data.remote

import com.google.gson.annotations.SerializedName

/** Root response for GET /api_category.php */
data class CategoriesResponse(
    @SerializedName("trivia_categories") val categories: List<CategoryDto>
)

data class CategoryDto(
    val id: Int,
    val name: String
)

/** Root response for GET /api.php */
data class QuestionsResponse(
    @SerializedName("response_code") val responseCode: Int,
    val results: List<QuestionDto>
)

/**
 * Raw question from OpenTDB.
 * HTML encoding in [question], [correctAnswer], and [incorrectAnswers] must be decoded
 * before storing — handled in [QuestionRepositoryImpl].
 */
data class QuestionDto(
    val type: String,
    val difficulty: String,
    val category: String,
    val question: String,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("incorrect_answers") val incorrectAnswers: List<String>
)
