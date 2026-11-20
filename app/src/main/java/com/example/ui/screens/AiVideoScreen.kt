package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.ProviderConfig
import com.example.domain.model.AspectRatioOption
import com.example.domain.model.CreditCalculator
import com.example.domain.model.VideoDuration
import com.example.domain.model.VideoFrameRate
import com.example.domain.model.VideoGenerationMode
import com.example.domain.model.VideoGenerationSpec
import com.example.domain.model.VideoProvider
import com.example.domain.model.VideoResolution
import com.example.localization.LocalAppStrings
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.Media3VideoPlayer
import com.example.ui.components.SpecBadge
import com.example.ui.components.rememberPhotoPicker
import com.example.ui.components.rememberVideoPicker
import com.example.ui.viewmodel.GenerationStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiVideoScreen(
    initialPrompt: String,
    generationStatus: GenerationStatus,
    onGenerateVideo: (VideoGenerationSpec) -> Unit,
    onEnhancePrompt: (String, String, (String) -> Unit) -> Unit,
    onSavePrompt: (String, String, String) -> Unit,
    onImportReference: (android.net.Uri, String, (String) -> Unit) -> Unit,
    onSaveToDevice: (String, String) -> Unit,
    onSendToEditor: (String) -> Unit,
    onCreateShareIntent: (String, String, String) -> Intent?
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var prompt by remember { mutableStateOf(initialPrompt) }
    LaunchedEffect(initialPrompt) {
        if (initialPrompt.isNotBlank()) prompt = initialPrompt
    }

    var selectedMode by remember { mutableStateOf(VideoGenerationMode.TEXT_TO_VIDEO) }
    var selectedProvider by remember { mutableStateOf(VideoProvider.RUNWAY_GEN4) }
    var selectedResolution by remember { mutableStateOf(VideoResolution.FHD_1080P) }
    var selectedFps by remember { mutableStateOf(VideoFrameRate.FPS_30) }
    var selectedRatio by remember { mutableStateOf(AspectRatioOption.LANDSCAPE_16_9) }
    var selectedDuration by remember { mutableStateOf(VideoDuration.SEC_10) }
    var customWidth by remember { mutableStateOf("1920") }
    var customHeight by remember { mutableStateOf("1080") }
    var cameraMotion by remember { mutableStateOf("Smooth Orbital Dolly") }
    var refImageName by remember { mutableStateOf<String?>(null) }
    var refImagePath by remember { mutableStateOf<String?>(null) }
    var sourceVideoName by remember { mutableStateOf<String?>(null) }
    var sourceVideoPath by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberPhotoPicker { uri ->
        onImportReference(uri, "PHOTO") { localPath ->
            refImagePath = localPath
            refImageName = localPath.substringAfterLast('/')
        }
    }
    val pickVideo = rememberVideoPicker { uri ->
        onImportReference(uri, "VIDEO") { localPath ->
            sourceVideoPath = localPath
            sourceVideoName = localPath.substringAfterLast('/')
        }
    }

    val estimatedCost = CreditCalculator.videoCost(selectedDuration, selectedResolution, selectedFps)
    val usesEnhancement = selectedProvider.needsMedia3Enhancement(selectedResolution, selectedFps)

    val buildSpec = {
        VideoGenerationSpec(
            prompt = prompt,
            mode = selectedMode,
            provider = selectedProvider,
            resolution = selectedResolution,
            frameRate = selectedFps,
            aspectRatio = selectedRatio,
            customWidth = customWidth.toIntOrNull() ?: 1920,
            customHeight = customHeight.toIntOrNull() ?: 1080,
            duration = selectedDuration,
            referenceImageUri = refImagePath,
            sourceVideoUri = sourceVideoPath,
            cameraMotion = cameraMotion
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navVideo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // 1. Creation Mode Selector (12 modes)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.selectMode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VideoGenerationMode.entries) { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("video_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 2. Prompt + Reference Media
        item {
            GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Video Prompt") },
                    placeholder = { Text(strings.promptHintVideo) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_prompt_input")
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onEnhancePrompt(prompt, selectedMode.displayName) { enhanced -> prompt = enhanced }
                        },
                        modifier = Modifier.testTag("video_enhance_prompt_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.enhancePrompt)
                    }
                    OutlinedButton(
                        onClick = pickPhoto,
                        modifier = Modifier.testTag("video_upload_ref_button")
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(refImageName ?: strings.uploadReference)
                    }
                    OutlinedButton(
                        onClick = pickVideo,
                        modifier = Modifier.testTag("video_upload_source_button")
                    ) {
                        Icon(Icons.Rounded.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sourceVideoName ?: strings.uploadVideo)
                    }
                    TextButton(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                onSavePrompt(prompt.take(28), selectedMode.displayName, prompt)
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Prompt")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cameraMotion,
                    onValueChange = { cameraMotion = it },
                    label = { Text("Camera Movement / Lens") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Modular AI Provider Selector
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.selectProvider, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoProvider.entries.forEach { provider ->
                        val configured = when (provider) {
                            VideoProvider.GOOGLE_VEO -> ProviderConfig.isFirebaseAiLogicEnabled
                            VideoProvider.RUNWAY_GEN4 -> ProviderConfig.isRunwayConfigured()
                            VideoProvider.KLING_AI -> ProviderConfig.isKlingConfigured()
                            VideoProvider.OPENAI_SORA_COMPAT -> ProviderConfig.isOpenAiConfigured()
                        }
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            leadingIcon = {
                                if (selectedProvider == provider) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${provider.displayName} (${provider.modelName})")
                                    SpecBadge(
                                        text = if (configured) "READY" else "NEEDS SETUP",
                                        accent = if (configured) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_provider_${provider.id}")
                        )
                    }
                }
            }
        }

        // 4. Video Quality Settings (720p..4K, 24..60fps, Aspect Ratio, Duration)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.qualitySettings, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                Text(strings.resolution, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoResolution.entries.forEach { res ->
                        FilterChip(
                            selected = selectedResolution == res,
                            onClick = { selectedResolution = res },
                            label = { Text(res.label) },
                            modifier = Modifier.testTag("video_res_${res.name.lowercase()}")
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(strings.frameRate, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoFrameRate.entries.forEach { fps ->
                        FilterChip(
                            selected = selectedFps == fps,
                            onClick = { selectedFps = fps },
                            label = { Text(fps.label) },
                            modifier = Modifier.testTag("video_fps_${fps.fps}")
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(strings.aspectRatio, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        FilterChip(
                            selected = selectedRatio == ratio,
                            onClick = { selectedRatio = ratio },
                            label = { Text(ratio.label) }
                        )
                    }
                }
                if (selectedRatio == AspectRatioOption.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        OutlinedTextField(
                            value = customWidth,
                            onValueChange = { customWidth = it.filter(Char::isDigit) },
                            label = { Text("Width") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customHeight,
                            onValueChange = { customHeight = it.filter(Char::isDigit) },
                            label = { Text("Height") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(strings.duration, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoDuration.entries.forEach { dur ->
                        FilterChip(
                            selected = selectedDuration == dur,
                            onClick = { selectedDuration = dur },
                            label = { Text(dur.label) }
                        )
                    }
                }

                if (usesEnhancement) {
                    Spacer(Modifier.height(8.dp))
                    SpecBadge(text = strings.enhancementPipelineNote, accent = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // 5. Credit Cost Preview + Generate CTA
        item {
            GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimated Generation Cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$estimatedCost ${strings.creditsLabel}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    GlowActionButton(
                        text = strings.generateVideo,
                        onClick = { onGenerateVideo(buildSpec()) },
                        enabled = generationStatus !is GenerationStatus.Running,
                        testTag = "generate_video_button",
                        leadingIcon = { Icon(Icons.Rounded.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // 6. Real-Time Generation Status & Preview Player
        item {
            when (generationStatus) {
                is GenerationStatus.Running -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(generationStatus.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { generationStatus.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${generationStatus.progress}%", style = MaterialTheme.typography.labelMedium)
                    }
                }
                is GenerationStatus.Completed -> {
                    val project = generationStatus.project
                    val mediaTarget = project.localFilePath ?: project.remoteUrl.orEmpty()
                    GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        if (mediaTarget.isNotBlank()) {
                            Media3VideoPlayer(mediaUriOrPath = mediaTarget)
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    onSaveToDevice(mediaTarget, "${project.title}.mp4")
                                }) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.saveToDevice)
                                }
                                OutlinedButton(onClick = { onSendToEditor(mediaTarget) }) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.navEditor)
                                }
                                OutlinedButton(onClick = { onGenerateVideo(buildSpec()) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.regenerate)
                                }
                                OutlinedButton(onClick = {
                                    onCreateShareIntent(mediaTarget, "video/mp4", project.title)?.let { intent ->
                                        context.startActivity(Intent.createChooser(intent, strings.share))
                                    }
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.share)
                                }
                            }
                        }
                    }
                }
                is GenerationStatus.Error -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = generationStatus.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                GenerationStatus.Idle -> Unit
            }
        }
    }
}
