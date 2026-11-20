package com.example.domain.model

enum class VideoGenerationMode(
    val displayName: String,
    val promptPrefix: String,
    val needsReferenceImage: Boolean = false,
    val needsSourceVideo: Boolean = false
) {
    TEXT_TO_VIDEO("Text-to-Video", "Cinematic ultra-detailed motion:"),
    IMAGE_TO_VIDEO("Image-to-Video", "Animate reference image smoothly:", needsReferenceImage = true),
    VIDEO_TO_VIDEO("Video-to-Video", "Stylize and transform motion video:", needsSourceVideo = true),
    AI_CINEMATIC("AI Cinematic", "35mm anamorphic cinema shot, dramatic lighting:"),
    REALISTIC_HUMAN("Human Animation", "Photorealistic human motion, natural facial expression:"),
    PRODUCT_AD("Product Ad", "Commercial luxury studio product reveal, orbital camera:"),
    SOCIAL_REELS("Social Reels", "High-energy vertical viral reel, dynamic cuts:"),
    MOVIE_TRAILER("Movie Trailer", "Epic theatrical trailer shot, volumetric atmosphere:"),
    ANIMATION_3D("3D Animation", "Stylized 3D CGI render, subsurface scattering:"),
    CAMERA_MOTION("Camera Motion", "Controlled dolly zoom and sweeping gimbal orbit:", needsReferenceImage = true),
    STORY_TO_VIDEO("Story-to-Video", "Multi-beat narrative sequence with continuity:"),
    REFERENCE_VIDEO("Reference-Based", "Subject-consistent generation from reference:", needsReferenceImage = true)
}

enum class VideoResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val creditMultiplier: Float
) {
    HD_720P("720p HD", 1280, 720, 1.0f),
    FHD_1080P("1080p Full HD", 1920, 1080, 1.5f),
    QHD_2K("2K QHD", 2560, 1440, 2.2f),
    UHD_4K("4K Ultra HD", 3840, 2160, 3.0f)
}

enum class VideoFrameRate(val fps: Int, val label: String, val creditAdd: Int) {
    FPS_24(24, "24 FPS", 0),
    FPS_30(30, "30 FPS", 2),
    FPS_50(50, "50 FPS", 5),
    FPS_60(60, "60 FPS", 8)
}

enum class AspectRatioOption(val label: String, val ratioValue: Float, val apiString: String) {
    PORTRAIT_9_16("9:16 Portrait", 9f / 16f, "9:16"),
    LANDSCAPE_16_9("16:9 Landscape", 16f / 9f, "16:9"),
    SQUARE_1_1("1:1 Square", 1f, "1:1"),
    SOCIAL_4_5("4:5 Feed", 4f / 5f, "4:5"),
    CINEMA_21_9("21:9 Cinema", 21f / 9f, "21:9"),
    CUSTOM("Custom", 16f / 9f, "custom")
}

enum class VideoDuration(val seconds: Int, val label: String, val baseCredits: Int) {
    SEC_5(5, "5s", 10),
    SEC_10(10, "10s", 18),
    SEC_15(15, "15s", 26),
    SEC_30(30, "30s", 45),
    SEC_60(60, "60s", 80)
}

enum class VideoProvider(
    val id: String,
    val displayName: String,
    val modelName: String,
    val nativeMaxResolution: VideoResolution,
    val nativeMaxFps: Int,
    val nativeMaxDurationSec: Int,
    val secretKeyName: String,
    val requiresFirebaseAiLogic: Boolean = false
) {
    GOOGLE_VEO(
        id = "veo",
        displayName = "Google Veo 3",
        modelName = "veo-3.0-generate-preview",
        nativeMaxResolution = VideoResolution.FHD_1080P,
        nativeMaxFps = 30,
        nativeMaxDurationSec = 15,
        secretKeyName = "FIREBASE_AI_LOGIC",
        requiresFirebaseAiLogic = true
    ),
    RUNWAY_GEN4(
        id = "runway",
        displayName = "Runway Gen-4",
        modelName = "gen4_turbo",
        nativeMaxResolution = VideoResolution.FHD_1080P,
        nativeMaxFps = 24,
        nativeMaxDurationSec = 10,
        secretKeyName = "RUNWAY_API_KEY"
    ),
    KLING_AI(
        id = "kling",
        displayName = "Kling AI 2.0",
        modelName = "kling-v2-master",
        nativeMaxResolution = VideoResolution.FHD_1080P,
        nativeMaxFps = 30,
        nativeMaxDurationSec = 10,
        secretKeyName = "KLING_API_KEY"
    ),
    OPENAI_SORA_COMPAT(
        id = "openai_compat",
        displayName = "OpenAI / Compatible",
        modelName = "sora-compat-1",
        nativeMaxResolution = VideoResolution.FHD_1080P,
        nativeMaxFps = 30,
        nativeMaxDurationSec = 15,
        secretKeyName = "OPENAI_API_KEY"
    );

    fun needsMedia3Enhancement(resolution: VideoResolution, fps: VideoFrameRate): Boolean {
        return resolution.ordinal > nativeMaxResolution.ordinal || fps.fps > nativeMaxFps
    }
}

enum class ImageStudioMode(val displayName: String, val creditCost: Int, val needsInputImage: Boolean = false) {
    TEXT_TO_IMAGE("Text-to-Image", 4, false),
    IMAGE_TO_IMAGE("Image-to-Image", 5, true),
    AI_PHOTO_EDIT("AI Photo Editing", 5, true),
    BG_REMOVAL("Background Removal", 3, true),
    UPSCALE_4K("Image Upscaling 4K", 4, true),
    FACE_CHARACTER_REF("Character Reference", 6, true),
    PRODUCT_PHOTOGRAPHY("Product Photography", 5, false),
    REALISTIC_PORTRAIT("Realistic Portrait", 5, false),
    CINEMATIC_PHOTO("Cinematic Photo", 5, false)
}

enum class ImageProvider(
    val id: String,
    val displayName: String,
    val modelName: String,
    val requiresFirebaseAiLogic: Boolean = false,
    val secretKeyName: String = ""
) {
    GEMINI_NANO_BANANA("gemini_image", "Gemini Nano Banana 2.1", "gemini-nano-banana-2.1", requiresFirebaseAiLogic = true),
    OPENAI_IMAGE("openai_image", "OpenAI GPT-Image-1", "gpt-image-1", secretKeyName = "OPENAI_API_KEY")
}

enum class ChatProvider(
    val id: String,
    val displayName: String,
    val modelName: String,
    val requiresFirebaseAiLogic: Boolean = false,
    val secretKeyName: String = ""
) {
    GEMINI_FLASH("gemini_flash", "Gemini 3.8 Flash", "gemini-3.8-flash", requiresFirebaseAiLogic = true),
    OPENAI_COMPAT("openai_chat", "OpenAI-Compatible LLM", "gpt-4o-mini", secretKeyName = "OPENAI_API_KEY")
}

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val monthlyCredits: Int,
    val priceUsd: String,
    val maxResolution: String,
    val priorityQueue: Boolean
) {
    FREE("free", "Free Plan", 50, "$0/mo", "1080p HD", false),
    BASIC("basic", "Basic Plan", 300, "$12/mo", "2K QHD", false),
    PRO("pro", "Pro Plan", 1200, "$29/mo", "4K 60fps", true),
    PREMIUM("premium", "Premium Ultra", 4000, "$79/mo", "4K 60fps + Priority", true);

    companion object {
        fun fromId(id: String): SubscriptionPlan = entries.firstOrNull { it.id == id } ?: FREE
    }
}

object CreditCalculator {
    fun videoCost(
        duration: VideoDuration,
        resolution: VideoResolution,
        fps: VideoFrameRate
    ): Int {
        return (duration.baseCredits * resolution.creditMultiplier).toInt() + fps.creditAdd
    }

    fun imageCost(mode: ImageStudioMode, export4K: Boolean): Int {
        return mode.creditCost + if (export4K) 3 else 0
    }
}

data class VideoGenerationSpec(
    val prompt: String,
    val mode: VideoGenerationMode,
    val provider: VideoProvider,
    val resolution: VideoResolution,
    val frameRate: VideoFrameRate,
    val aspectRatio: AspectRatioOption,
    val customWidth: Int = 1920,
    val customHeight: Int = 1080,
    val duration: VideoDuration,
    val referenceImageUri: String? = null,
    val sourceVideoUri: String? = null,
    val cameraMotion: String = "Smooth Orbit"
) {
    val creditCost: Int
        get() = CreditCalculator.videoCost(duration, resolution, frameRate)
}
