package com.example.data.remote

import com.example.BuildConfig

data class ProviderStatus(
    val id: String,
    val name: String,
    val isConfigured: Boolean,
    val configMethod: String
)

object ProviderConfig {
    fun runwayApiKey(): String = safeKey(BuildConfig.RUNWAY_API_KEY)
    fun klingApiKey(): String = safeKey(BuildConfig.KLING_API_KEY)
    fun openAiApiKey(): String = safeKey(BuildConfig.OPENAI_API_KEY)

    fun isRunwayConfigured(): Boolean = runwayApiKey().isNotEmpty()
    fun isKlingConfigured(): Boolean = klingApiKey().isNotEmpty()
    fun isOpenAiConfigured(): Boolean = openAiApiKey().isNotEmpty()

    // Firebase was declined by the user, so Firebase AI Logic is not provisioned.
    const val isFirebaseAiLogicEnabled: Boolean = false

    fun allStatuses(): List<ProviderStatus> = listOf(
        ProviderStatus(
            id = "gemini_veo",
            name = "Google Gemini / Veo (Firebase AI Logic)",
            isConfigured = isFirebaseAiLogicEnabled,
            configMethod = "Enable Firebase AI Logic in AI Studio"
        ),
        ProviderStatus(
            id = "runway",
            name = "Runway Gen-4 Video",
            isConfigured = isRunwayConfigured(),
            configMethod = "Set RUNWAY_API_KEY in AI Studio Secrets"
        ),
        ProviderStatus(
            id = "kling",
            name = "Kling AI 2.0 Video",
            isConfigured = isKlingConfigured(),
            configMethod = "Set KLING_API_KEY in AI Studio Secrets"
        ),
        ProviderStatus(
            id = "openai",
            name = "OpenAI-Compatible (Chat & Images)",
            isConfigured = isOpenAiConfigured(),
            configMethod = "Set OPENAI_API_KEY in AI Studio Secrets"
        )
    )

    private fun safeKey(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return if (trimmed.isBlank() || trimmed.startsWith("MY_")) "" else trimmed
    }
}
