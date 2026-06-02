package com.hz.appon.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit interface for the Open Trivia Database API. */
interface OpenTdbApi {

    /**
     * Fetches all available trivia categories.
     * Call once on first launch to populate the local category list.
     */
    @GET("api_category.php")
    suspend fun getCategories(): CategoriesResponse

    /**
     * Fetches questions for a specific category and difficulty.
     *
     * @param amount Number of questions to fetch (max 50 per request)
     * @param categoryId OpenTDB category ID
     * @param difficulty "easy", "medium", or "hard"
     * @param type Always "multiple" — we only use 4-option questions
     */
    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int,
        @Query("category") categoryId: Int,
        @Query("difficulty") difficulty: String,
        @Query("type") type: String = "multiple"
    ): QuestionsResponse

    companion object {
        private const val BASE_URL = "https://opentdb.com/"

        /** Creates a configured [OpenTdbApi] instance. */
        fun create(): OpenTdbApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenTdbApi::class.java)
    }
}
