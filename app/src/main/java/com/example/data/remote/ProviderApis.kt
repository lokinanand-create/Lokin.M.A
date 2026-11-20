package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Runway Gen-4 API DTOs ---
@JsonClass(generateAdapter = true)
data class RunwayCreateTaskRequest(
    val model: String = "gen4_turbo",
    @Json(name = "promptText") val promptText: String,
    @Json(name = "promptImage") val promptImage: String? = null,
    val ratio: String = "1280:720",
    val duration: Int = 5
)

@JsonClass(generateAdapter = true)
data class RunwayTaskResponse(
    val id: String,
    val status: String? = null,
    val progress: Double? = null,
    val output: List<String>? = null,
    val failure: String? = null
)

interface RunwayApiService {
    @POST("image_to_video")
    suspend fun createImageToVideo(
        @Header("Authorization") bearer: String,
        @Header("X-Runway-Version") version: String = "2024-11-06",
        @Body request: RunwayCreateTaskRequest
    ): Response<RunwayTaskResponse>

    @POST("text_to_video")
    suspend fun createTextToVideo(
        @Header("Authorization") bearer: String,
        @Header("X-Runway-Version") version: String = "2024-11-06",
        @Body request: RunwayCreateTaskRequest
    ): Response<RunwayTaskResponse>

    @GET("tasks/{id}")
    suspend fun getTaskStatus(
        @Header("Authorization") bearer: String,
        @Header("X-Runway-Version") version: String = "2024-11-06",
        @Path("id") taskId: String
    ): Response<RunwayTaskResponse>
}

// --- Kling AI API DTOs ---
@JsonClass(generateAdapter = true)
data class KlingCreateRequest(
    @Json(name = "model_name") val modelName: String = "kling-v2-master",
    val prompt: String,
    @Json(name = "aspect_ratio") val aspectRatio: String = "16:9",
    val duration: String = "5"
)

@JsonClass(generateAdapter = true)
data class KlingTaskData(
    @Json(name = "task_id") val taskId: String? = null,
    @Json(name = "task_status") val taskStatus: String? = null,
    @Json(name = "task_status_msg") val statusMessage: String? = null,
    @Json(name = "task_result") val taskResult: KlingTaskResult? = null
)

@JsonClass(generateAdapter = true)
data class KlingTaskResult(
    val videos: List<KlingVideoItem>? = null
)

@JsonClass(generateAdapter = true)
data class KlingVideoItem(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class KlingApiResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: KlingTaskData? = null
)

interface KlingApiService {
    @POST("videos/text2video")
    suspend fun createTextToVideo(
        @Header("Authorization") bearer: String,
        @Body request: KlingCreateRequest
    ): Response<KlingApiResponse>

    @GET("videos/text2video/{id}")
    suspend fun getTaskStatus(
        @Header("Authorization") bearer: String,
        @Path("id") taskId: String
    ): Response<KlingApiResponse>
}

// --- OpenAI-Compatible API DTOs ---
@JsonClass(generateAdapter = true)
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAiChatMessage>,
    val temperature: Double = 0.7
)

@JsonClass(generateAdapter = true)
data class OpenAiChatChoice(
    val message: OpenAiChatMessage? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val choices: List<OpenAiChatChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiImageRequest(
    val model: String = "gpt-image-1",
    val prompt: String,
    val size: String = "1024x1024",
    val n: Int = 1
)

@JsonClass(generateAdapter = true)
data class OpenAiImageData(
    val url: String? = null,
    @Json(name = "b64_json") val b64Json: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiImageResponse(
    val data: List<OpenAiImageData>? = null
)

interface OpenAiCompatibleService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") bearer: String,
        @Body request: OpenAiChatRequest
    ): Response<OpenAiChatResponse>

    @POST("images/generations")
    suspend fun generateImage(
        @Header("Authorization") bearer: String,
        @Body request: OpenAiImageRequest
    ): Response<OpenAiImageResponse>
}

object NetworkClients {
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    val runwayService: RunwayApiService by lazy {
        retrofit("https://api.dev.runwayml.com/v1/").create(RunwayApiService::class.java)
    }

    val klingService: KlingApiService by lazy {
        retrofit("https://api.klingai.com/v1/").create(KlingApiService::class.java)
    }

    fun openAiService(baseUrl: String): OpenAiCompatibleService {
        return retrofit(baseUrl).create(OpenAiCompatibleService::class.java)
    }
}
