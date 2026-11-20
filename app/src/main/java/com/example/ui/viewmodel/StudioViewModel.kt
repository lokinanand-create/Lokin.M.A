package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.CreditTransactionEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SavedPromptEntity
import com.example.data.local.StudioDatabase
import com.example.data.local.StudioPreferences
import com.example.data.local.StudioSettingsSnapshot
import com.example.data.local.UploadedMediaEntity
import com.example.data.local.UserAccountEntity
import com.example.data.remote.StudioMediaPipeline
import com.example.data.remote.VideoEditConfig
import com.example.data.repository.InsufficientCreditsException
import com.example.data.repository.ProviderSetupRequiredException
import com.example.data.repository.StudioRepository
import com.example.domain.model.ChatProvider
import com.example.domain.model.ImageProvider
import com.example.domain.model.ImageStudioMode
import com.example.domain.model.SubscriptionPlan
import com.example.domain.model.VideoGenerationSpec
import com.example.localization.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GenerationStatus {
    data object Idle : GenerationStatus
    data class Running(val progress: Int, val label: String) : GenerationStatus
    data class Completed(val project: ProjectEntity) : GenerationStatus
    data class Error(val message: String) : GenerationStatus
}

data class PendingConfirmation(
    val title: String,
    val credits: Int,
    val summary: String,
    val onConfirm: () -> Unit
)

data class SetupAlertState(
    val providerName: String,
    val secretKeyName: String,
    val requiresFirebase: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(private val repository: StudioRepository) : ViewModel() {

    val settings: StateFlow<StudioSettingsSnapshot> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioSettingsSnapshot())

    val currentAccount: StateFlow<UserAccountEntity?> = settings
        .flatMapLatest { s ->
            val email = s.signedInEmail
            if (email.isNullOrBlank()) flowOf(null) else repository.observeAccount(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projects: StateFlow<List<ProjectEntity>> = settings
        .flatMapLatest { s ->
            val email = s.signedInEmail
            if (email.isNullOrBlank()) flowOf(emptyList()) else repository.observeProjects(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPrompts: StateFlow<List<SavedPromptEntity>> = settings
        .flatMapLatest { s ->
            val email = s.signedInEmail
            if (email.isNullOrBlank()) flowOf(emptyList()) else repository.observePrompts(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uploadedMedia: StateFlow<List<UploadedMediaEntity>> = settings
        .flatMapLatest { s ->
            val email = s.signedInEmail
            if (email.isNullOrBlank()) flowOf(emptyList()) else repository.observeMedia(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditTransactions: StateFlow<List<CreditTransactionEntity>> = settings
        .flatMapLatest { s ->
            val email = s.signedInEmail
            if (email.isNullOrBlank()) flowOf(emptyList()) else repository.observeTransactions(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatSessions: StateFlow<List<ChatSessionEntity>> = settings
        .flatMapLatest { s ->
            val email = s.signedInEmail
            if (email.isNullOrBlank()) flowOf(emptyList()) else repository.observeSessions(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessageEntity>> = _activeSessionId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _videoGenStatus = MutableStateFlow<GenerationStatus>(GenerationStatus.Idle)
    val videoGenStatus: StateFlow<GenerationStatus> = _videoGenStatus.asStateFlow()

    private val _imageGenStatus = MutableStateFlow<GenerationStatus>(GenerationStatus.Idle)
    val imageGenStatus: StateFlow<GenerationStatus> = _imageGenStatus.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _editorProgress = MutableStateFlow<Int?>(null)
    val editorProgress: StateFlow<Int?> = _editorProgress.asStateFlow()

    private val _exportedVideoPath = MutableStateFlow<String?>(null)
    val exportedVideoPath: StateFlow<String?> = _exportedVideoPath.asStateFlow()

    private val _synthesizedVoicePath = MutableStateFlow<String?>(null)
    val synthesizedVoicePath: StateFlow<String?> = _synthesizedVoicePath.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _setupAlert = MutableStateFlow<SetupAlertState?>(null)
    val setupAlert: StateFlow<SetupAlertState?> = _setupAlert.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private fun emailOrNull(): String? = settings.value.signedInEmail

    // --- Auth ---
    fun register(email: String, name: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.registerAccount(email, name, password)
                .onFailure { _authError.value = it.localizedMessage ?: "Registration failed" }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.signIn(email, password)
                .onFailure { _authError.value = it.localizedMessage ?: "Sign in failed" }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _authError.value = null
            repository.continueAsStudioGuest()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _activeSessionId.value = null
            repository.signOut()
        }
    }

    // --- Preferences & Language ---
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setDarkMode(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.preferences.setLanguage(language)
            _snackbarMessage.tryEmit("Language: ${language.nativeName}")
        }
    }

    fun setWatermarkEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setWatermarkEnabled(enabled) }
    }

    fun setPrivacyMode(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setPrivacyMode(enabled) }
    }

    fun updateOpenAiEndpoint(baseUrl: String, model: String) {
        viewModelScope.launch {
            repository.preferences.setOpenAiEndpoint(baseUrl, model)
            _snackbarMessage.tryEmit("OpenAI-compatible endpoint updated")
        }
    }

    // --- Confirmation & Alerts ---
    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    fun dismissSetupAlert() {
        _setupAlert.value = null
    }

    // --- Video Generation Workflow ---
    fun requestVideoGeneration(spec: VideoGenerationSpec) {
        if (spec.prompt.isBlank()) {
            _snackbarMessage.tryEmit("Please enter a prompt first.")
            return
        }
        val enhancementNote = if (spec.provider.needsMedia3Enhancement(spec.resolution, spec.frameRate)) {
            " (includes Media3 ${spec.resolution.label}/${spec.frameRate.fps}fps pipeline)"
        } else ""

        _pendingConfirmation.value = PendingConfirmation(
            title = "${spec.mode.displayName} • ${spec.provider.displayName}",
            credits = spec.creditCost,
            summary = "${spec.resolution.label}, ${spec.frameRate.label}, ${spec.aspectRatio.apiString}, ${spec.duration.label}$enhancementNote",
            onConfirm = {
                _pendingConfirmation.value = null
                executeVideoGeneration(spec)
            }
        )
    }

    private fun executeVideoGeneration(spec: VideoGenerationSpec) {
        val email = emailOrNull() ?: return
        viewModelScope.launch {
            _videoGenStatus.value = GenerationStatus.Running(15, "Dispatching to ${spec.provider.displayName}...")
            val result = repository.generateVideo(email, spec)
            result.onSuccess { project ->
                _videoGenStatus.value = GenerationStatus.Completed(project)
                _snackbarMessage.tryEmit("Video generation completed!")
            }.onFailure { err ->
                when (err) {
                    is ProviderSetupRequiredException -> {
                        _videoGenStatus.value = GenerationStatus.Idle
                        _setupAlert.value = SetupAlertState(err.providerName, err.secretKeyName, err.requiresFirebase)
                    }
                    is InsufficientCreditsException -> {
                        _videoGenStatus.value = GenerationStatus.Error(err.message ?: "Insufficient credits")
                    }
                    is CancellationException -> throw err
                    else -> {
                        _videoGenStatus.value = GenerationStatus.Error(err.localizedMessage ?: "Generation failed")
                    }
                }
            }
        }
    }

    fun enhancePrompt(rawPrompt: String, modeName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.aiRepository.enhanceVideoPrompt(rawPrompt, modeName, settings.value.language)
            onResult(res.text)
            res.notice?.let { _snackbarMessage.tryEmit(it) }
        }
    }

    // --- Image Generation Studio ---
    fun requestImageGeneration(
        prompt: String,
        mode: ImageStudioMode,
        provider: ImageProvider,
        export4K: Boolean,
        cost: Int
    ) {
        if (prompt.isBlank()) {
            _snackbarMessage.tryEmit("Please enter an image prompt.")
            return
        }
        _pendingConfirmation.value = PendingConfirmation(
            title = "${mode.displayName} • ${provider.displayName}",
            credits = cost,
            summary = if (export4K) "4K Ultra HD Export" else "1K Studio Quality",
            onConfirm = {
                _pendingConfirmation.value = null
                executeImageGeneration(prompt, mode, provider, export4K, cost)
            }
        )
    }

    private fun executeImageGeneration(
        prompt: String,
        mode: ImageStudioMode,
        provider: ImageProvider,
        export4K: Boolean,
        cost: Int
    ) {
        val email = emailOrNull() ?: return
        viewModelScope.launch {
            _imageGenStatus.value = GenerationStatus.Running(35, "Synthesizing with ${provider.displayName}...")
            repository.generateImage(email, prompt, mode, provider, export4K, cost)
                .onSuccess { project ->
                    _imageGenStatus.value = GenerationStatus.Completed(project)
                    _snackbarMessage.tryEmit("Image created!")
                }
                .onFailure { err ->
                    when (err) {
                        is ProviderSetupRequiredException -> {
                            _imageGenStatus.value = GenerationStatus.Idle
                            _setupAlert.value = SetupAlertState(err.providerName, err.secretKeyName, err.requiresFirebase)
                        }
                        is CancellationException -> throw err
                        else -> _imageGenStatus.value = GenerationStatus.Error(err.localizedMessage ?: "Image generation failed")
                    }
                }
        }
    }

    // --- AI Chat ---
    fun selectChatSession(sessionId: Long) {
        _activeSessionId.value = sessionId
    }

    fun startNewChatSession(provider: ChatProvider) {
        val email = emailOrNull() ?: return
        viewModelScope.launch {
            val id = repository.createChatSession(email, provider)
            _activeSessionId.value = id
        }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_activeSessionId.value == sessionId) _activeSessionId.value = null
        }
    }

    fun sendChatMessage(
        text: String,
        provider: ChatProvider,
        attachmentUri: String? = null,
        attachmentName: String? = null
    ) {
        val email = emailOrNull() ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val sessionId = _activeSessionId.value ?: repository.createChatSession(email, provider).also {
                _activeSessionId.value = it
            }
            _streamingReply.value = ""
            try {
                repository.sendChatMessage(
                    sessionId = sessionId,
                    userText = text,
                    provider = provider,
                    attachmentUri = attachmentUri,
                    attachmentName = attachmentName,
                    language = settings.value.language,
                    history = activeMessages.value
                ).collect { chunk ->
                    _streamingReply.value = chunk.text
                }
                _streamingReply.value = null
            } catch (e: ProviderSetupRequiredException) {
                _streamingReply.value = null
                _setupAlert.value = SetupAlertState(e.providerName, e.secretKeyName, e.requiresFirebase)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _streamingReply.value = null
                _snackbarMessage.tryEmit(e.localizedMessage ?: "Chat failed")
            }
        }
    }

    fun regenerateLastResponse(userMessage: ChatMessageEntity, provider: ChatProvider) {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            _streamingReply.value = ""
            try {
                repository.regenerateFromMessage(
                    sessionId = sessionId,
                    userMessage = userMessage,
                    provider = provider,
                    language = settings.value.language,
                    history = activeMessages.value
                ).collect { chunk ->
                    _streamingReply.value = chunk.text
                }
                _streamingReply.value = null
            } catch (e: ProviderSetupRequiredException) {
                _streamingReply.value = null
                _setupAlert.value = SetupAlertState(e.providerName, e.secretKeyName, e.requiresFirebase)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _streamingReply.value = null
                _snackbarMessage.tryEmit(e.localizedMessage ?: "Regeneration failed")
            }
        }
    }

    // --- Video Editor (Media3 Transformer & TTS) ---
    fun exportEditedVideo(title: String, config: VideoEditConfig) {
        val email = emailOrNull() ?: return
        viewModelScope.launch {
            _editorProgress.value = 1
            val res = repository.mediaPipeline.exportEditedVideo(config) { pct ->
                _editorProgress.value = pct
            }
            _editorProgress.value = null
            res.onSuccess { outPath ->
                _exportedVideoPath.value = outPath
                repository.saveEditedProject(email, title, config, outPath)
                _snackbarMessage.tryEmit("Exported MP4 successfully!")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Video export failed")
            }
        }
    }

    fun generateVoiceOverScript(topic: String, onScript: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.aiRepository.generateVoiceOverScript(topic, 10, settings.value.language)
            onScript(res.text)
            res.notice?.let { _snackbarMessage.tryEmit(it) }
        }
    }

    fun synthesizeVoiceOver(text: String) {
        viewModelScope.launch {
            repository.mediaPipeline.synthesizeVoiceOverToFile(text, settings.value.language)
                .onSuccess { path ->
                    _synthesizedVoicePath.value = path
                    _snackbarMessage.tryEmit("Voice-over WAV synthesized!")
                }
                .onFailure { err ->
                    _snackbarMessage.tryEmit(err.localizedMessage ?: "Voice synthesis failed")
                }
        }
    }

    // --- Media, Prompts, Projects, Subscriptions ---
    fun importMedia(uri: Uri, fallbackCategory: String, onImported: ((UploadedMediaEntity) -> Unit)? = null) {
        val email = emailOrNull() ?: return
        viewModelScope.launch {
            repository.importMedia(email, uri, fallbackCategory)
                .onSuccess {
                    _snackbarMessage.tryEmit("Imported ${it.displayName}")
                    onImported?.invoke(it)
                }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Import failed") }
        }
    }

    fun replaceMedia(existing: UploadedMediaEntity, newUri: Uri) {
        viewModelScope.launch {
            repository.replaceMedia(existing, newUri)
                .onSuccess { _snackbarMessage.tryEmit("Replaced with ${it.displayName}") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Replace failed") }
        }
    }

    fun deleteMedia(item: UploadedMediaEntity) {
        viewModelScope.launch {
            repository.deleteMedia(item)
            _snackbarMessage.tryEmit("Deleted ${item.displayName}")
        }
    }

    fun saveToDeviceGallery(localPathOrUrl: String, isVideo: Boolean, displayName: String) {
        viewModelScope.launch {
            val result = if (localPathOrUrl.startsWith("http://") || localPathOrUrl.startsWith("https://")) {
                _snackbarMessage.tryEmit("Downloading to device Gallery...")
                repository.mediaPipeline.downloadRemoteUrlToDevice(localPathOrUrl, isVideo, displayName)
            } else {
                repository.mediaPipeline.saveFileToDeviceGallery(localPathOrUrl, isVideo, displayName)
            }
            result
                .onSuccess { _snackbarMessage.tryEmit("Saved to device Gallery!") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Save failed") }
        }
    }

    fun createShareIntent(localPathOrUri: String, mimeType: String, title: String): Intent? {
        return repository.mediaPipeline.createShareIntent(localPathOrUri, mimeType, title)
    }

    fun savePrompt(title: String, category: String, text: String) {
        val email = emailOrNull() ?: return
        viewModelScope.launch {
            repository.savePrompt(email, title, category, text)
            _snackbarMessage.tryEmit("Prompt saved!")
        }
    }

    fun deletePrompt(id: Long) {
        viewModelScope.launch { repository.deletePrompt(id) }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch { repository.deleteProject(id) }
    }

    fun requestPlanUpgrade(plan: SubscriptionPlan) {
        val email = emailOrNull() ?: return
        _pendingConfirmation.value = PendingConfirmation(
            title = "Switch to ${plan.title}",
            credits = 0,
            summary = "Demo billing mode: grants +${plan.monthlyCredits} studio credits locally. Production requires Google Play Billing Library.",
            onConfirm = {
                _pendingConfirmation.value = null
                viewModelScope.launch {
                    repository.upgradePlan(email, plan)
                        .onSuccess { _snackbarMessage.tryEmit("${plan.title} activated (+${plan.monthlyCredits} credits)") }
                }
            }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = StudioDatabase.getInstance(app)
                val prefs = StudioPreferences(app)
                val pipeline = StudioMediaPipeline(app)
                val repo = StudioRepository(app, db, prefs, pipeline)
                StudioViewModel(repo)
            }
        }
    }
}
