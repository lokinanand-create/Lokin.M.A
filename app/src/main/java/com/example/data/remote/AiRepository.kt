package com.example.data.remote

import com.example.localization.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class AiTextResult(
    val text: String,
    val isPlaceholderSample: Boolean,
    val notice: String? = null
)

interface AiRepository {
    val requiresFirebaseSetup: Boolean
    fun streamChatReply(
        prompt: String,
        historySummary: String,
        attachmentName: String?,
        language: AppLanguage
    ): Flow<AiTextResult>

    suspend fun enhanceVideoPrompt(rawPrompt: String, modeName: String, language: AppLanguage): AiTextResult
    suspend fun generateVoiceOverScript(topic: String, durationSec: Int, language: AppLanguage): AiTextResult
}

/**
 * Fallback placeholder implementation used when Firebase setup was declined by the user.
 * Returns clearly labeled sample responses without embedding any Gemini API key or direct REST client.
 */
class PlaceholderGeminiAiRepository : AiRepository {
    override val requiresFirebaseSetup: Boolean = true

    override fun streamChatReply(
        prompt: String,
        historySummary: String,
        attachmentName: String?,
        language: AppLanguage
    ): Flow<AiTextResult> = flow {
        val attachmentNote = if (attachmentName != null) " [Attached: $attachmentName]" else ""
        val sampleResponse = buildString {
            append("[Sample Gemini AI Response — Requires Firebase AI Logic]\n\n")
            append("Prompt received$attachmentNote: \"${prompt.take(140)}\"\n")
            append("Language mode: ${language.nativeName}\n\n")
            append("Recommended Cinematic Video Prompt:\n")
            append("• Subject & Action: Dynamic subject reveal with subtle atmospheric haze and crisp rim lighting.\n")
            append("• Camera: 35mm anamorphic lens, slow pushing dolly-in at 24fps, shallow depth of field.\n")
            append("• Color & Mood: Deep obsidian shadows with electric violet and cyan highlights.\n\n")
            append("Note: Enable Firebase in AI Studio to connect live Gemini 3.8 Flash & Nano Banana 2.1 models.")
        }

        val words = sampleResponse.split(" ")
        val sb = StringBuilder()
        for ((index, word) in words.withIndex()) {
            sb.append(if (index == 0) word else " $word")
            emit(
                AiTextResult(
                    text = sb.toString(),
                    isPlaceholderSample = true,
                    notice = "Sample response: Enable Firebase to activate live Gemini AI Logic."
                )
            )
            delay(18)
        }
    }

    override suspend fun enhanceVideoPrompt(
        rawPrompt: String,
        modeName: String,
        language: AppLanguage
    ): AiTextResult {
        val clean = rawPrompt.trim().ifBlank { "Futuristic neon cityscape at twilight" }
        return AiTextResult(
            text = "[Sample Enhanced Prompt — Firebase Required] $modeName: $clean — Shot on 35mm anamorphic prime, smooth stabilized gimbal push-in, volumetric purple-blue lighting, hyper-detailed textures, photorealistic motion.",
            isPlaceholderSample = true,
            notice = "Enable Firebase AI Logic for live Gemini prompt enhancement."
        )
    }

    override suspend fun generateVoiceOverScript(
        topic: String,
        durationSec: Int,
        language: AppLanguage
    ): AiTextResult {
        val clean = topic.trim().ifBlank { "MA AI Studio showcase" }
        return AiTextResult(
            text = "[Sample Script (${durationSec}s) — Firebase Required] Step into the future of visual storytelling with $clean. Every frame crafted with precision, motion, and radiant color.",
            isPlaceholderSample = true,
            notice = "Sample script shown. Enable Firebase AI Logic for live Gemini script generation."
        )
    }
}
