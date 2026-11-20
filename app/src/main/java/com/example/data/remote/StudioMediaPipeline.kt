package com.example.data.remote

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class ImportedFileResult(
    val displayName: String,
    val mimeType: String,
    val localPath: String,
    val sizeBytes: Long,
    val mediaCategory: String
)

data class VideoEditConfig(
    val inputUris: List<String>,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val contrast: Float = 0f,       // -1f..1f
    val saturation: Float = 0f,     // -100f..100f
    val brightness: Float = 0f,     // -100f..100f
    val overlayText: String = "",
    val subtitleText: String = "",
    val includeWatermark: Boolean = true,
    val removeOriginalAudio: Boolean = false,
    val targetWidth: Int = 1920,
    val targetHeight: Int = 1080,
    val speedMultiplier: Float = 1.0f,
    val backgroundAudioPath: String? = null,
    val transitionStyle: String = "Crossfade"
)

class StudioMediaPipeline(private val context: Context) {

    private val mediaDir: File by lazy {
        File(context.filesDir, "media").apply { mkdirs() }
    }

    private val exportsDir: File by lazy {
        File(context.filesDir, "exports").apply { mkdirs() }
    }

    private val allowedExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "mp4", "mov", "mkv", "webm", "mp3", "wav", "m4a", "aac", "pdf", "txt"
    )

    suspend fun importPickedUri(uri: Uri, fallbackCategory: String): Result<ImportedFileResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                var fileName = "studio_${System.currentTimeMillis()}"
                var reportedSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0) reportedSize = cursor.getLong(sizeIdx)
                    }
                }

                val ext = fileName.substringAfterLast('.', "").lowercase()
                val inferredExt = ext.ifBlank {
                    when {
                        mimeType.startsWith("image/png") -> "png"
                        mimeType.startsWith("image/webp") -> "webp"
                        mimeType.startsWith("image/") -> "jpg"
                        mimeType.startsWith("video/") -> "mp4"
                        mimeType.startsWith("audio/") -> "mp3"
                        mimeType == "application/pdf" -> "pdf"
                        else -> "bin"
                    }
                }

                if (inferredExt !in allowedExtensions && inferredExt != "bin") {
                    throw IllegalArgumentException("Unsupported file extension: .$inferredExt")
                }

                val maxBytes = if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
                    200L * 1024L * 1024L
                } else {
                    25L * 1024L * 1024L
                }
                if (reportedSize > maxBytes) {
                    throw IllegalArgumentException("File exceeds maximum allowed size.")
                }

                val safeName = "${UUID.randomUUID().toString().take(8)}_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
                val targetFile = File(mediaDir, if (safeName.contains('.')) safeName else "$safeName.$inferredExt")

                resolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Unable to read selected file.")

                val category = when {
                    mimeType.startsWith("image/") -> "PHOTO"
                    mimeType.startsWith("video/") -> "VIDEO"
                    mimeType.startsWith("audio/") -> "AUDIO"
                    mimeType == "application/pdf" || mimeType.startsWith("text/") -> "DOCUMENT"
                    else -> fallbackCategory
                }

                ImportedFileResult(
                    displayName = fileName,
                    mimeType = mimeType,
                    localPath = targetFile.absolutePath,
                    sizeBytes = targetFile.length(),
                    mediaCategory = category
                )
            }
        }

    @OptIn(UnstableApi::class)
    suspend fun exportEditedVideo(
        config: VideoEditConfig,
        onProgress: (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.Main) {
        if (config.inputUris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Select at least one video clip to export."))
        }

        val outputFile = File(exportsDir, "ma_studio_${System.currentTimeMillis()}.mp4")
        val mainHandler = Handler(Looper.getMainLooper())

        suspendCancellableCoroutine { continuation ->
            try {
                val videoEffects = buildList<Effect> {
                    if (config.targetWidth > 0 && config.targetHeight > 0) {
                        add(
                            Presentation.createForWidthAndHeight(
                                config.targetWidth,
                                config.targetHeight,
                                Presentation.LAYOUT_SCALE_TO_FIT
                            )
                        )
                    }
                    if (config.contrast != 0f) {
                        add(Contrast(config.contrast.coerceIn(-0.9f, 0.9f)))
                    }
                    if (config.saturation != 0f || config.brightness != 0f) {
                        add(
                            HslAdjustment.Builder()
                                .adjustSaturation(config.saturation.coerceIn(-100f, 100f))
                                .adjustLightness(config.brightness.coerceIn(-100f, 100f))
                                .build()
                        )
                    }

                    val overlays = buildList {
                        val combinedOverlay = listOf(config.overlayText.trim(), config.subtitleText.trim())
                            .filter { it.isNotEmpty() }
                            .joinToString("\n")
                        if (combinedOverlay.isNotEmpty()) {
                            val span = SpannableString(combinedOverlay).apply {
                                setSpan(ForegroundColorSpan(android.graphics.Color.WHITE), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                setSpan(RelativeSizeSpan(1.4f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            add(
                                TextOverlay.createStaticTextOverlay(
                                    span,
                                    StaticOverlaySettings.Builder()
                                        .setOverlayFrameAnchor(0f, -0.72f)
                                        .build()
                                )
                            )
                        }
                        if (config.includeWatermark) {
                            val wm = SpannableString("MA AI STUDIO").apply {
                                setSpan(ForegroundColorSpan(0xCCDDD6FE.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                setSpan(RelativeSizeSpan(0.9f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            add(
                                TextOverlay.createStaticTextOverlay(
                                    wm,
                                    StaticOverlaySettings.Builder()
                                        .setOverlayFrameAnchor(0.72f, 0.85f)
                                        .build()
                                )
                            )
                        }
                    }
                    if (overlays.isNotEmpty()) {
                        add(OverlayEffect(overlays))
                    }
                }

                val editedItems = config.inputUris.mapIndexed { index, uriStr ->
                    val itemBuilder = MediaItem.Builder().setUri(uriStr)
                    if (index == 0 && (config.trimStartMs > 0L || (config.trimEndMs > config.trimStartMs && config.trimEndMs > 0L))) {
                        val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(config.trimStartMs.coerceAtLeast(0L))
                        if (config.trimEndMs > config.trimStartMs && config.trimEndMs > 0L) {
                            clipBuilder.setEndPositionMs(config.trimEndMs)
                        }
                        itemBuilder.setClippingConfiguration(clipBuilder.build())
                    }
                    EditedMediaItem.Builder(itemBuilder.build())
                        .setRemoveAudio(config.removeOriginalAudio)
                        .setEffects(Effects(emptyList(), videoEffects))
                        .build()
                }

                val sequences = buildList {
                    add(EditedMediaItemSequence.Builder(editedItems).build())
                    val bgPath = config.backgroundAudioPath
                    if (!bgPath.isNullOrBlank()) {
                        val bgFile = File(bgPath)
                        if (bgFile.exists()) {
                            val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(bgFile)))
                                .setRemoveVideo(true)
                                .build()
                            add(
                                EditedMediaItemSequence.Builder(listOf(audioItem))
                                    .setIsLooping(true)
                                    .build()
                            )
                        }
                    }
                }
                val composition = Composition.Builder(sequences).build()

                var transformer: Transformer? = null
                val progressHolder = ProgressHolder()
                var finished = false

                val progressRunnable = object : Runnable {
                    override fun run() {
                        if (finished) return
                        val t = transformer ?: return
                        val state = t.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress.coerceIn(0, 99))
                        }
                        mainHandler.postDelayed(this, 300L)
                    }
                }

                transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            finished = true
                            mainHandler.removeCallbacks(progressRunnable)
                            onProgress(100)
                            if (continuation.isActive) {
                                continuation.resume(Result.success(outputFile.absolutePath))
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            finished = true
                            mainHandler.removeCallbacks(progressRunnable)
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(exportException))
                            }
                        }
                    })
                    .build()

                transformer.start(composition, outputFile.absolutePath)
                mainHandler.postDelayed(progressRunnable, 300L)

                continuation.invokeOnCancellation {
                    finished = true
                    mainHandler.removeCallbacks(progressRunnable)
                    mainHandler.post { transformer.cancel() }
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    suspend fun synthesizeVoiceOverToFile(text: String, language: AppLanguage): Result<String> =
        withContext(Dispatchers.Main) {
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Enter voice-over text first."))
            }
            val outFile = File(mediaDir, "voiceover_${System.currentTimeMillis()}.wav")
            suspendCancellableCoroutine { continuation ->
                var tts: TextToSpeech? = null
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status != TextToSpeech.SUCCESS) {
                        tts?.shutdown()
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(IllegalStateException("Device TTS engine unavailable.")))
                        }
                        return@TextToSpeech
                    }
                    val locale = when (language) {
                        AppLanguage.ENGLISH -> Locale.US
                        AppLanguage.TAMIL -> Locale("ta", "IN")
                        AppLanguage.HINDI -> Locale("hi", "IN")
                        AppLanguage.MALAYALAM -> Locale("ml", "IN")
                        AppLanguage.KANNADA -> Locale("kn", "IN")
                        AppLanguage.TELUGU -> Locale("te", "IN")
                    }
                    tts?.language = locale
                    val utteranceId = UUID.randomUUID().toString()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) {
                            tts?.shutdown()
                            if (continuation.isActive) {
                                continuation.resume(Result.success(outFile.absolutePath))
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            tts?.shutdown()
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(IllegalStateException("TTS synthesis failed.")))
                            }
                        }
                    })
                    val res = tts?.synthesizeToFile(text, null, outFile, utteranceId)
                    if (res != TextToSpeech.SUCCESS) {
                        tts?.shutdown()
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(IllegalStateException("Failed to start TTS export.")))
                        }
                    }
                }
                continuation.invokeOnCancellation { tts?.shutdown() }
            }
        }

    suspend fun saveFileToDeviceGallery(localPath: String, isVideo: Boolean, displayName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val srcFile = File(localPath)
                require(srcFile.exists()) { "Source file does not exist on device." }
                val collection = if (isVideo) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            if (isVideo) Environment.DIRECTORY_MOVIES + "/MA_AI_Studio"
                            else Environment.DIRECTORY_PICTURES + "/MA_AI_Studio"
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val destUri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("MediaStore failed to allocate output URI.")

                resolver.openOutputStream(destUri)?.use { out ->
                    srcFile.inputStream().use { inp -> inp.copyTo(out) }
                } ?: throw IllegalStateException("Failed to open destination stream.")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(destUri, values, null, null)
                }
                destUri.toString()
            }
        }

    suspend fun downloadRemoteUrlToDevice(remoteUrl: String, isVideo: Boolean, displayName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()
                val req = Request.Builder().url(remoteUrl).build()
                val ext = if (isVideo) "mp4" else "png"
                val tempFile = File(mediaDir, "dl_${System.currentTimeMillis()}.$ext")
                try {
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            throw IllegalStateException("Download failed with HTTP ${resp.code}")
                        }
                        val body = resp.body ?: throw IllegalStateException("Empty download response body.")
                        body.byteStream().use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    saveFileToDeviceGallery(tempFile.absolutePath, isVideo, displayName).getOrThrow()
                } finally {
                    runCatching { tempFile.delete() }
                }
            }
        }

    fun createShareIntent(localPathOrUri: String, mimeType: String, title: String): Intent? {
        return try {
            val uri = if (localPathOrUri.startsWith("http://") || localPathOrUri.startsWith("https://")) {
                return Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, localPathOrUri)
                }
            } else {
                val file = File(localPathOrUri)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    Uri.parse(localPathOrUri)
                }
            }
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: Exception) {
            null
        }
    }
}
