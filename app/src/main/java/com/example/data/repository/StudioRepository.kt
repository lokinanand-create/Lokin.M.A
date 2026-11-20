package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.CreditDao
import com.example.data.local.CreditTransactionEntity
import com.example.data.local.MediaDao
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.PromptDao
import com.example.data.local.SavedPromptEntity
import com.example.data.local.StudioDatabase
import com.example.data.local.StudioPreferences
import com.example.data.local.StudioSettingsSnapshot
import com.example.data.local.UploadedMediaEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.UserDao
import com.example.data.remote.AiRepository
import com.example.data.remote.AiTextResult
import com.example.data.remote.ImportedFileResult
import com.example.data.remote.KlingCreateRequest
import com.example.data.remote.NetworkClients
import com.example.data.remote.OpenAiChatMessage
import com.example.data.remote.OpenAiChatRequest
import com.example.data.remote.OpenAiImageRequest
import com.example.data.remote.PlaceholderGeminiAiRepository
import com.example.data.remote.ProviderConfig
import com.example.data.remote.RunwayCreateTaskRequest
import com.example.data.remote.StudioMediaPipeline
import com.example.data.remote.VideoEditConfig
import com.example.domain.model.ChatProvider
import com.example.domain.model.ImageProvider
import com.example.domain.model.ImageStudioMode
import com.example.domain.model.SubscriptionPlan
import com.example.domain.model.VideoGenerationSpec
import com.example.domain.model.VideoProvider
import com.example.localization.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class ProviderSetupRequiredException(
    val providerName: String,
    val secretKeyName: String,
    val requiresFirebase: Boolean
) : Exception(
    if (requiresFirebase) "$providerName requires Firebase AI Logic to be enabled."
    else "$providerName requires $secretKeyName in Google AI Studio Secrets."
)

class InsufficientCreditsException(val required: Int, val available: Int) :
    Exception("Insufficient credits: requires $required, available $available.")

class StudioRepository(
    private val context: Context,
    private val db: StudioDatabase,
    val preferences: StudioPreferences,
    val mediaPipeline: StudioMediaPipeline,
    val aiRepository: AiRepository = PlaceholderGeminiAiRepository()
) {
    private val projectDao: ProjectDao = db.projectDao()
    private val chatDao: ChatDao = db.chatDao()
    private val promptDao: PromptDao = db.promptDao()
    private val mediaDao: MediaDao = db.mediaDao()
    private val creditDao: CreditDao = db.creditDao()
    private val userDao: UserDao = db.userDao()

    private var lastRequestEpochMs = 0L

    val settingsFlow: Flow<StudioSettingsSnapshot> = preferences.settingsFlow

    fun observeAccount(email: String): Flow<UserAccountEntity?> = userDao.observeAccount(email)
    fun observeProjects(email: String): Flow<List<ProjectEntity>> = projectDao.observeProjects(email)
    fun observeSessions(email: String): Flow<List<ChatSessionEntity>> = chatDao.observeSessions(email)
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>> = chatDao.observeMessages(sessionId)
    fun observePrompts(email: String): Flow<List<SavedPromptEntity>> = promptDao.observePrompts(email)
    fun observeMedia(email: String): Flow<List<UploadedMediaEntity>> = mediaDao.observeMedia(email)
    fun observeTransactions(email: String): Flow<List<CreditTransactionEntity>> = creditDao.observeTransactions(email)

    // --- Authentication (Local Account Store; Cloud Google Sign-In requires Firebase) ---
    suspend fun registerAccount(email: String, displayName: String, password: String): Result<UserAccountEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanEmail = email.trim().lowercase()
                require(cleanEmail.contains("@") && cleanEmail.length >= 5) { "Enter a valid email address." }
                require(displayName.isNotBlank()) { "Enter your creator name." }
                require(password.length >= 6) { "Password must be at least 6 characters." }
                if (userDao.getAccount(cleanEmail) != null) {
                    throw IllegalStateException("An account with this email already exists. Please sign in.")
                }
                val account = UserAccountEntity(
                    email = cleanEmail,
                    displayName = displayName.trim(),
                    passwordHash = sha256(password),
                    planId = SubscriptionPlan.FREE.id,
                    credits = SubscriptionPlan.FREE.monthlyCredits
                )
                userDao.upsertAccount(account)
                creditDao.insertTransaction(
                    CreditTransactionEntity(
                        ownerEmail = cleanEmail,
                        description = "Free Plan Welcome Bonus",
                        deltaCredits = 50,
                        balanceAfter = 50
                    )
                )
                seedStarterPrompts(cleanEmail)
                preferences.setSignedInEmail(cleanEmail)
                account
            }
        }

    suspend fun signIn(email: String, password: String): Result<UserAccountEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanEmail = email.trim().lowercase()
                val existing = userDao.getAccount(cleanEmail)
                    ?: throw IllegalStateException("Account not found. Please register first.")
                if (existing.passwordHash != sha256(password)) {
                    throw IllegalArgumentException("Incorrect password.")
                }
                preferences.setSignedInEmail(cleanEmail)
                existing
            }
        }

    suspend fun continueAsStudioGuest(): UserAccountEntity = withContext(Dispatchers.IO) {
        val guestEmail = "studio.creator@local"
        val existing = userDao.getAccount(guestEmail)
        val account = existing ?: UserAccountEntity(
            email = guestEmail,
            displayName = "Studio Creator",
            passwordHash = sha256("local_guest"),
            planId = SubscriptionPlan.FREE.id,
            credits = 50
        ).also {
            userDao.upsertAccount(it)
            creditDao.insertTransaction(
                CreditTransactionEntity(
                    ownerEmail = guestEmail,
                    description = "Free Plan Welcome Bonus",
                    deltaCredits = 50,
                    balanceAfter = 50
                )
            )
            seedStarterPrompts(guestEmail)
        }
        preferences.setSignedInEmail(guestEmail)
        account
    }

    suspend fun signOut() {
        preferences.setSignedInEmail(null)
    }

    private suspend fun seedStarterPrompts(email: String) {
        val starters = listOf(
            SavedPromptEntity(ownerEmail = email, title = "Cyberpunk Neon Trailer", category = "Movie Trailer", promptText = "Anamorphic 35mm tracking shot through a rain-slicked neon obsidian metropolis, violet reflections, volumetric fog, dramatic trailer pacing."),
            SavedPromptEntity(ownerEmail = email, title = "Luxury Headphones Ad", category = "Product Ad", promptText = "Matte-black wireless headphones levitating above dark obsidian glass, dramatic purple rim lighting, slow 360-degree orbital macro camera."),
            SavedPromptEntity(ownerEmail = email, title = "Viral Vertical Reel", category = "Social Reels", promptText = "Dynamic 9:16 lifestyle transformation with smooth speed-ramp match cuts, vibrant studio glow, crisp motion clarity.")
        )
        starters.forEach { promptDao.insertPrompt(it) }
    }

    // --- Credits & Subscription ---
    suspend fun upgradePlan(email: String, plan: SubscriptionPlan): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val account = userDao.getAccount(email) ?: error("Account missing")
                val newBalance = account.credits + plan.monthlyCredits
                userDao.upsertAccount(account.copy(planId = plan.id, credits = newBalance))
                creditDao.insertTransaction(
                    CreditTransactionEntity(
                        ownerEmail = email,
                        description = "Plan Updated: ${plan.title} (+${plan.monthlyCredits} credits)",
                        deltaCredits = plan.monthlyCredits,
                        balanceAfter = newBalance
                    )
                )
            }
        }

    private suspend fun deductCredits(email: String, amount: Int, reason: String) {
        val account = userDao.getAccount(email) ?: error("Account not found")
        if (account.credits < amount) {
            throw InsufficientCreditsException(required = amount, available = account.credits)
        }
        val nextBalance = account.credits - amount
        userDao.upsertAccount(account.copy(credits = nextBalance))
        creditDao.insertTransaction(
            CreditTransactionEntity(
                ownerEmail = email,
                description = reason,
                deltaCredits = -amount,
                balanceAfter = nextBalance
            )
        )
    }

    // --- Video Generation Workflow (Real API + Honest Setup Enforcement) ---
    suspend fun generateVideo(email: String, spec: VideoGenerationSpec): Result<ProjectEntity> =
        withContext(Dispatchers.IO) {
            try {
                enforceRateLimit()
                val account = userDao.getAccount(email) ?: error("Account missing")
                if (account.credits < spec.creditCost) {
                    return@withContext Result.failure(InsufficientCreditsException(spec.creditCost, account.credits))
                }

                // Check provider readiness BEFORE charging credits!
                if (spec.provider.requiresFirebaseAiLogic && !ProviderConfig.isFirebaseAiLogicEnabled) {
                    return@withContext Result.failure(
                        ProviderSetupRequiredException(spec.provider.displayName, "FIREBASE_AI_LOGIC", true)
                    )
                }
                when (spec.provider) {
                    VideoProvider.RUNWAY_GEN4 -> if (!ProviderConfig.isRunwayConfigured()) {
                        return@withContext Result.failure(
                            ProviderSetupRequiredException(spec.provider.displayName, spec.provider.secretKeyName, false)
                        )
                    }
                    VideoProvider.KLING_AI -> if (!ProviderConfig.isKlingConfigured()) {
                        return@withContext Result.failure(
                            ProviderSetupRequiredException(spec.provider.displayName, spec.provider.secretKeyName, false)
                        )
                    }
                    VideoProvider.OPENAI_SORA_COMPAT -> if (!ProviderConfig.isOpenAiConfigured()) {
                        return@withContext Result.failure(
                            ProviderSetupRequiredException(spec.provider.displayName, spec.provider.secretKeyName, false)
                        )
                    }
                    VideoProvider.GOOGLE_VEO -> {}
                }

                val usesEnhancement = spec.provider.needsMedia3Enhancement(spec.resolution, spec.frameRate)
                val projectId = projectDao.insertProject(
                    ProjectEntity(
                        ownerEmail = email,
                        title = "${spec.mode.displayName}: ${spec.prompt.take(28)}",
                        type = "VIDEO",
                        mode = spec.mode.displayName,
                        provider = spec.provider.displayName,
                        prompt = spec.prompt,
                        status = "PROCESSING",
                        progressPercent = 10,
                        resolution = spec.resolution.label,
                        fps = spec.frameRate.fps,
                        aspectRatio = spec.aspectRatio.apiString,
                        durationSec = spec.duration.seconds,
                        enhancementPipelineUsed = usesEnhancement,
                        creditsCharged = spec.creditCost
                    )
                )

                val remoteResultUrl = retryTransient {
                    when (spec.provider) {
                        VideoProvider.RUNWAY_GEN4 -> executeRunwayVideo(spec) { pct ->
                            projectDao.updateProject(
                                ProjectEntity(
                                    id = projectId, ownerEmail = email,
                                    title = "${spec.mode.displayName}: ${spec.prompt.take(28)}",
                                    type = "VIDEO", mode = spec.mode.displayName, provider = spec.provider.displayName,
                                    prompt = spec.prompt, status = "PROCESSING", progressPercent = pct,
                                    resolution = spec.resolution.label, fps = spec.frameRate.fps,
                                    aspectRatio = spec.aspectRatio.apiString, durationSec = spec.duration.seconds,
                                    enhancementPipelineUsed = usesEnhancement, creditsCharged = spec.creditCost
                                )
                            )
                        }
                        VideoProvider.KLING_AI -> executeKlingVideo(spec) { pct ->
                            projectDao.updateProject(
                                ProjectEntity(
                                    id = projectId, ownerEmail = email,
                                    title = "${spec.mode.displayName}: ${spec.prompt.take(28)}",
                                    type = "VIDEO", mode = spec.mode.displayName, provider = spec.provider.displayName,
                                    prompt = spec.prompt, status = "PROCESSING", progressPercent = pct,
                                    resolution = spec.resolution.label, fps = spec.frameRate.fps,
                                    aspectRatio = spec.aspectRatio.apiString, durationSec = spec.duration.seconds,
                                    enhancementPipelineUsed = usesEnhancement, creditsCharged = spec.creditCost
                                )
                            )
                        }
                        VideoProvider.OPENAI_SORA_COMPAT -> throw IllegalStateException(
                            "OpenAI-compatible /v1/videos endpoint requires a configured Sora-compatible video backend; Runway Gen-4 and Kling AI video endpoints plus OpenAI Chat & Image endpoints are active."
                        )
                        VideoProvider.GOOGLE_VEO -> throw ProviderSetupRequiredException(
                            spec.provider.displayName,
                            spec.provider.secretKeyName,
                            true
                        )
                    }
                }

                deductCredits(email, spec.creditCost, "Video: ${spec.mode.displayName} (${spec.resolution.label}, ${spec.frameRate.fps}fps)")

                var finalLocalPath: String? = null
                if (usesEnhancement) {
                    val enhanced = mediaPipeline.exportEditedVideo(
                        VideoEditConfig(
                            inputUris = listOf(remoteResultUrl),
                            targetWidth = spec.resolution.width,
                            targetHeight = spec.resolution.height,
                            includeWatermark = preferences.settingsFlow.first().watermarkEnabled
                        )
                    ) {}
                    finalLocalPath = enhanced.getOrNull()
                }

                val completed = ProjectEntity(
                    id = projectId,
                    ownerEmail = email,
                    title = "${spec.mode.displayName}: ${spec.prompt.take(28)}",
                    type = "VIDEO",
                    mode = spec.mode.displayName,
                    provider = spec.provider.displayName,
                    prompt = spec.prompt,
                    status = "COMPLETED",
                    progressPercent = 100,
                    resolution = spec.resolution.label,
                    fps = spec.frameRate.fps,
                    aspectRatio = spec.aspectRatio.apiString,
                    durationSec = spec.duration.seconds,
                    enhancementPipelineUsed = usesEnhancement,
                    localFilePath = finalLocalPath,
                    remoteUrl = remoteResultUrl,
                    creditsCharged = spec.creditCost
                )
                projectDao.updateProject(completed)
                Result.success(completed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun executeRunwayVideo(spec: VideoGenerationSpec, onProgress: suspend (Int) -> Unit): String {
        val bearer = "Bearer ${ProviderConfig.runwayApiKey()}"
        val ratio = when (spec.aspectRatio) {
            com.example.domain.model.AspectRatioOption.PORTRAIT_9_16 -> "720:1280"
            com.example.domain.model.AspectRatioOption.SQUARE_1_1 -> "960:960"
            else -> "1280:720"
        }
        val request = RunwayCreateTaskRequest(
            model = spec.provider.modelName,
            promptText = "${spec.mode.promptPrefix} ${spec.prompt}",
            ratio = ratio,
            duration = spec.duration.seconds.coerceIn(5, 10)
        )
        val createResp = NetworkClients.runwayService.createTextToVideo(bearer = bearer, request = request)
        if (!createResp.isSuccessful) {
            throw IllegalStateException("Runway API error ${createResp.code()}: ${createResp.errorBody()?.string().orEmpty()}")
        }
        val taskId = createResp.body()?.id ?: throw IllegalStateException("Runway returned empty task ID.")

        repeat(45) { attempt ->
            delay(3000L)
            val statusResp = NetworkClients.runwayService.getTaskStatus(bearer = bearer, taskId = taskId)
            val body = statusResp.body()
            val pct = ((body?.progress ?: (attempt / 45.0)) * 100).toInt().coerceIn(15, 95)
            onProgress(pct)
            when (body?.status?.uppercase()) {
                "SUCCEEDED" -> return body.output?.firstOrNull()
                    ?: throw IllegalStateException("Runway completed without output URL.")
                "FAILED" -> throw IllegalStateException(body.failure ?: "Runway generation failed.")
            }
        }
        throw IllegalStateException("Runway task timed out while polling.")
    }

    private suspend fun executeKlingVideo(spec: VideoGenerationSpec, onProgress: suspend (Int) -> Unit): String {
        val bearer = "Bearer ${ProviderConfig.klingApiKey()}"
        val createResp = NetworkClients.klingService.createTextToVideo(
            bearer = bearer,
            request = KlingCreateRequest(
                modelName = spec.provider.modelName,
                prompt = "${spec.mode.promptPrefix} ${spec.prompt}",
                aspectRatio = spec.aspectRatio.apiString,
                duration = spec.duration.seconds.coerceAtMost(10).toString()
            )
        )
        if (!createResp.isSuccessful) {
            throw IllegalStateException("Kling API error ${createResp.code()}: ${createResp.errorBody()?.string().orEmpty()}")
        }
        val taskId = createResp.body()?.data?.taskId ?: throw IllegalStateException("Kling returned empty task ID.")

        repeat(45) { attempt ->
            delay(3000L)
            val statusResp = NetworkClients.klingService.getTaskStatus(bearer = bearer, taskId = taskId)
            val taskData = statusResp.body()?.data
            onProgress((20 + attempt * 2).coerceAtMost(95))
            when (taskData?.taskStatus?.lowercase()) {
                "succeed" -> return taskData.taskResult?.videos?.firstOrNull()?.url
                    ?: throw IllegalStateException("Kling succeeded without video URL.")
                "failed" -> throw IllegalStateException(taskData.statusMessage ?: "Kling generation failed.")
            }
        }
        throw IllegalStateException("Kling task timed out while polling.")
    }

    // --- Image Generation Studio ---
    suspend fun generateImage(
        email: String,
        prompt: String,
        mode: ImageStudioMode,
        provider: ImageProvider,
        export4K: Boolean,
        cost: Int
    ): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        try {
            enforceRateLimit()
            val account = userDao.getAccount(email) ?: error("Account missing")
            if (account.credits < cost) {
                return@withContext Result.failure(InsufficientCreditsException(cost, account.credits))
            }

            if (provider.requiresFirebaseAiLogic && !ProviderConfig.isFirebaseAiLogicEnabled) {
                return@withContext Result.failure(
                    ProviderSetupRequiredException(provider.displayName, "FIREBASE_AI_LOGIC", true)
                )
            }
            if (provider == ImageProvider.OPENAI_IMAGE && !ProviderConfig.isOpenAiConfigured()) {
                return@withContext Result.failure(
                    ProviderSetupRequiredException(provider.displayName, provider.secretKeyName, false)
                )
            }

            val settings = preferences.settingsFlow.first()
            val service = NetworkClients.openAiService(settings.openAiBaseUrl)
            val resp = retryTransient {
                val res = service.generateImage(
                    bearer = "Bearer ${ProviderConfig.openAiApiKey()}",
                    request = OpenAiImageRequest(
                        model = provider.modelName,
                        prompt = "${mode.displayName}: $prompt",
                        size = "1024x1024"
                    )
                )
                if (!res.isSuccessful) {
                    throw IllegalStateException("Image API error ${res.code()}: ${res.errorBody()?.string().orEmpty()}")
                }
                res.body() ?: throw IllegalStateException("Empty image response.")
            }

            val dataItem = resp.data?.firstOrNull() ?: throw IllegalStateException("No image data returned.")
            var savedLocalPath: String? = null
            if (!dataItem.b64Json.isNullOrBlank()) {
                val bytes = Base64.decode(dataItem.b64Json, Base64.DEFAULT)
                val dir = File(context.filesDir, "media").apply { mkdirs() }
                val file = File(dir, "img_${System.currentTimeMillis()}.png")
                file.writeBytes(bytes)
                savedLocalPath = file.absolutePath
            }

            deductCredits(email, cost, "Image: ${mode.displayName}")
            val project = ProjectEntity(
                ownerEmail = email,
                title = "${mode.displayName}: ${prompt.take(26)}",
                type = "IMAGE",
                mode = mode.displayName,
                provider = provider.displayName,
                prompt = prompt,
                status = "COMPLETED",
                progressPercent = 100,
                resolution = if (export4K) "4K Ultra HD" else "1K HD",
                fps = 0,
                aspectRatio = "1:1",
                durationSec = 0,
                localFilePath = savedLocalPath,
                remoteUrl = dataItem.url,
                creditsCharged = cost
            )
            val id = projectDao.insertProject(project)
            Result.success(project.copy(id = id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- AI Chat ---
    suspend fun createChatSession(email: String, provider: ChatProvider, title: String = "New Studio Chat"): Long {
        return chatDao.insertSession(
            ChatSessionEntity(ownerEmail = email, title = title, providerId = provider.id)
        )
    }

    suspend fun deleteChatSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    fun sendChatMessage(
        sessionId: Long,
        userText: String,
        provider: ChatProvider,
        attachmentUri: String?,
        attachmentName: String?,
        language: AppLanguage,
        history: List<ChatMessageEntity>
    ): Flow<AiTextResult> = flow {
        chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = userText,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName
            )
        )
        chatDao.updateSessionTitle(sessionId, userText.take(36), System.currentTimeMillis())

        if (provider == ChatProvider.OPENAI_COMPAT) {
            if (!ProviderConfig.isOpenAiConfigured()) {
                throw ProviderSetupRequiredException(provider.displayName, provider.secretKeyName, false)
            }
            val settings = preferences.settingsFlow.first()
            val messages = history.takeLast(8).map {
                OpenAiChatMessage(role = it.role, content = it.content)
            } + OpenAiChatMessage(
                role = "user",
                content = "Respond in ${language.nativeName}. $userText" +
                    (if (attachmentName != null) " [Attached file: $attachmentName]" else "")
            )
            val resp = NetworkClients.openAiService(settings.openAiBaseUrl).chatCompletions(
                bearer = "Bearer ${ProviderConfig.openAiApiKey()}",
                request = OpenAiChatRequest(model = settings.openAiModel, messages = messages)
            )
            if (!resp.isSuccessful) {
                throw IllegalStateException("Chat API error ${resp.code()}: ${resp.errorBody()?.string().orEmpty()}")
            }
            val reply = resp.body()?.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty chat response.")
            chatDao.insertMessage(
                ChatMessageEntity(sessionId = sessionId, role = "assistant", content = reply, isSampleNotice = false)
            )
            emit(AiTextResult(text = reply, isPlaceholderSample = false))
        } else {
            var latestText = ""
            aiRepository.streamChatReply(
                prompt = userText,
                historySummary = history.takeLast(4).joinToString("\n") { "${it.role}: ${it.content}" },
                attachmentName = attachmentName,
                language = language
            ).collect { chunk ->
                latestText = chunk.text
                emit(chunk)
            }
            if (latestText.isNotBlank()) {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = latestText,
                        isSampleNotice = true
                    )
                )
            }
        }
    }

    suspend fun regenerateFromMessage(
        sessionId: Long,
        userMessage: ChatMessageEntity,
        provider: ChatProvider,
        language: AppLanguage,
        history: List<ChatMessageEntity>
    ): Flow<AiTextResult> {
        chatDao.deleteMessagesAfter(sessionId, userMessage.id)
        return flow {
            if (provider == ChatProvider.OPENAI_COMPAT) {
                if (!ProviderConfig.isOpenAiConfigured()) {
                    throw ProviderSetupRequiredException(provider.displayName, provider.secretKeyName, false)
                }
                val settings = preferences.settingsFlow.first()
                val resp = NetworkClients.openAiService(settings.openAiBaseUrl).chatCompletions(
                    bearer = "Bearer ${ProviderConfig.openAiApiKey()}",
                    request = OpenAiChatRequest(
                        model = settings.openAiModel,
                        messages = listOf(OpenAiChatMessage("user", "Respond in ${language.nativeName}. ${userMessage.content}"))
                    )
                )
                val reply = resp.body()?.choices?.firstOrNull()?.message?.content
                    ?: throw IllegalStateException("Empty chat response.")
                chatDao.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "assistant", content = reply))
                emit(AiTextResult(reply, false))
            } else {
                var latest = ""
                aiRepository.streamChatReply(userMessage.content, "", userMessage.attachmentName, language).collect {
                    latest = it.text
                    emit(it)
                }
                if (latest.isNotBlank()) {
                    chatDao.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "assistant", content = latest, isSampleNotice = true))
                }
            }
        }
    }

    // --- Media, Prompts & Editor Exports ---
    suspend fun importMedia(email: String, uri: Uri, fallbackCategory: String): Result<UploadedMediaEntity> {
        val res = mediaPipeline.importPickedUri(uri, fallbackCategory)
        return res.map { imported ->
            val entity = UploadedMediaEntity(
                ownerEmail = email,
                displayName = imported.displayName,
                mediaType = imported.mediaCategory,
                mimeType = imported.mimeType,
                localPath = imported.localPath,
                sizeBytes = imported.sizeBytes
            )
            val id = mediaDao.insertMedia(entity)
            entity.copy(id = id)
        }
    }

    suspend fun replaceMedia(existing: UploadedMediaEntity, newUri: Uri): Result<UploadedMediaEntity> {
        val res = mediaPipeline.importPickedUri(newUri, existing.mediaType)
        return res.map { imported ->
            runCatching { File(existing.localPath).delete() }
            val updated = existing.copy(
                displayName = imported.displayName,
                mimeType = imported.mimeType,
                localPath = imported.localPath,
                sizeBytes = imported.sizeBytes,
                mediaType = imported.mediaCategory
            )
            mediaDao.updateMedia(updated)
            updated
        }
    }

    suspend fun deleteMedia(item: UploadedMediaEntity) {
        runCatching { File(item.localPath).delete() }
        mediaDao.deleteMedia(item.id)
    }

    suspend fun saveEditedProject(email: String, title: String, config: VideoEditConfig, outputPath: String) {
        projectDao.insertProject(
            ProjectEntity(
                ownerEmail = email,
                title = title.ifBlank { "Studio Edit" },
                type = "EDIT",
                mode = "Media3 Timeline Edit",
                provider = "On-Device Media3 Transformer",
                prompt = config.overlayText.ifBlank { "Trim, Grade & Export" },
                status = "COMPLETED",
                progressPercent = 100,
                resolution = "${config.targetWidth}x${config.targetHeight}",
                fps = 30,
                aspectRatio = "Edited MP4",
                durationSec = 0,
                localFilePath = outputPath,
                creditsCharged = 0
            )
        )
    }

    suspend fun savePrompt(email: String, title: String, category: String, text: String) {
        promptDao.insertPrompt(
            SavedPromptEntity(ownerEmail = email, title = title, category = category, promptText = text)
        )
    }

    suspend fun deletePrompt(id: Long) = promptDao.deletePrompt(id)
    suspend fun deleteProject(id: Long) = projectDao.deleteProject(id)

    private suspend fun enforceRateLimit() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestEpochMs
        if (elapsed < 1200L) {
            delay(1200L - elapsed)
        }
        lastRequestEpochMs = System.currentTimeMillis()
    }

    private suspend fun <T> retryTransient(attempts: Int = 2, block: suspend () -> T): T {
        var lastErr: Exception? = null
        repeat(attempts) { idx ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastErr = e
                if (idx < attempts - 1) delay(1500L * (idx + 1))
            }
        }
        throw lastErr ?: IllegalStateException("Operation failed.")
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
