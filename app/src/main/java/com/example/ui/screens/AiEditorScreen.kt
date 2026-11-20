package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.VideoEditConfig
import com.example.localization.LocalAppStrings
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.Media3VideoPlayer
import com.example.ui.components.SpecBadge
import com.example.ui.components.rememberAudioPicker
import com.example.ui.components.rememberVideoPicker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiEditorScreen(
    initialVideoUri: String?,
    editorProgress: Int?,
    exportedVideoPath: String?,
    synthesizedVoicePath: String?,
    defaultWatermark: Boolean,
    onImportMedia: (Uri, String, (String) -> Unit) -> Unit,
    onExportVideo: (String, VideoEditConfig) -> Unit,
    onGenerateVoiceScript: (String, (String) -> Unit) -> Unit,
    onSynthesizeVoice: (String) -> Unit,
    onSaveToDevice: (String, String) -> Unit,
    onCreateShareIntent: (String, String, String) -> Intent?
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val timelineClips = remember { mutableStateListOf<String>() }
    LaunchedEffect(initialVideoUri) {
        if (!initialVideoUri.isNullOrBlank() && initialVideoUri !in timelineClips) {
            timelineClips.add(initialVideoUri)
        }
    }

    var projectTitle by remember { mutableStateOf("Studio Timeline Cut") }
    var trimStartSec by remember { mutableFloatStateOf(0f) }
    var trimEndSec by remember { mutableFloatStateOf(0f) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var overlayText by remember { mutableStateOf("") }
    var subtitleText by remember { mutableStateOf("") }
    var voiceScript by remember { mutableStateOf("") }
    var selectedTransition by remember { mutableStateOf("Crossfade") }
    var bgMusicName by remember { mutableStateOf<String?>(null) }
    var bgMusicPath by remember { mutableStateOf<String?>(null) }
    var audioSyncOffsetMs by remember { mutableFloatStateOf(0f) }
    var muteOriginalAudio by remember { mutableStateOf(false) }
    var includeWatermark by remember { mutableStateOf(defaultWatermark) }
    var export4K by remember { mutableStateOf(false) }

    val pickVideo = rememberVideoPicker { uri ->
        onImportMedia(uri, "VIDEO") { localPath -> timelineClips.add(localPath) }
    }
    val pickAudio = rememberAudioPicker { uri ->
        onImportMedia(uri, "AUDIO") { localPath ->
            bgMusicPath = localPath
            bgMusicName = localPath.substringAfterLast('/')
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navEditor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // 1. Timeline Clips + Preview Player
        item {
            GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { projectTitle = it },
                    label = { Text("Edit Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                if (timelineClips.isNotEmpty()) {
                    Media3VideoPlayer(
                        mediaUriOrPath = timelineClips.first(),
                        playbackSpeed = speedMultiplier
                    )
                    Spacer(Modifier.height(10.dp))
                } else {
                    Text(
                        text = "Pick one or more video clips from your device to edit, trim, merge, grade, and export.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = pickVideo,
                        modifier = Modifier.testTag("editor_pick_video_button")
                    ) {
                        Icon(
                            if (timelineClips.isEmpty()) Icons.Rounded.VideoLibrary else Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (timelineClips.isEmpty()) strings.uploadVideo else strings.mergeClips)
                    }
                    OutlinedButton(onClick = pickAudio, modifier = Modifier.testTag("editor_pick_audio_button")) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(bgMusicName ?: strings.uploadAudio)
                    }
                }

                if (timelineClips.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    timelineClips.forEachIndexed { idx, clip ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Clip ${idx + 1}: ${clip.substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { timelineClips.removeAt(idx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove clip", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. Trim, Speed & Transitions
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("${strings.trimClip} & ${strings.speedControl}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Trim Start: ${trimStartSec.toInt()}s | Trim End (0 = full): ${trimEndSec.toInt()}s", style = MaterialTheme.typography.labelMedium)
                Slider(value = trimStartSec, onValueChange = { trimStartSec = it }, valueRange = 0f..30f)
                Slider(value = trimEndSec, onValueChange = { trimEndSec = it }, valueRange = 0f..60f)
                if (trimEndSec > 0f && trimEndSec <= trimStartSec) {
                    Text(
                        text = "End time must be greater than Start time (or 0 for full clip).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text("Playback / Slow-Mo Speed: ${"%.2f".format(speedMultiplier)}x", style = MaterialTheme.typography.labelMedium)
                Slider(value = speedMultiplier, onValueChange = { speedMultiplier = it }, valueRange = 0.25f..2.0f)

                Spacer(Modifier.height(6.dp))
                Text("Scene Transition", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Crossfade", "Hard Cut", "Prism Wipe", "Flash Dissolve").forEach { tr ->
                        FilterChip(
                            selected = selectedTransition == tr,
                            onClick = { selectedTransition = tr },
                            label = { Text(tr) }
                        )
                    }
                }
            }
        }

        // 3. Color Grading & Video Enhancement
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.colorCorrection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Contrast: ${"%.2f".format(contrast)}", style = MaterialTheme.typography.labelMedium)
                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -0.6f..0.8f)

                Text("Saturation: ${saturation.toInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = -60f..60f)

                Text("Brightness: ${brightness.toInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -40f..40f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4K Ultra HD Upscale Pipeline", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = export4K, onCheckedChange = { export4K = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.watermark, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includeWatermark, onCheckedChange = { includeWatermark = it })
                }
            }
        }

        // 4. Text Overlays, Subtitles & Voice-Over Synthesis
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("${strings.textOverlay}, ${strings.subtitles} & ${strings.voiceOver}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = overlayText,
                    onValueChange = { overlayText = it },
                    label = { Text(strings.textOverlay) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = subtitleText,
                    onValueChange = { subtitleText = it },
                    label = { Text(strings.subtitles) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = voiceScript,
                    onValueChange = { voiceScript = it },
                    label = { Text("${strings.voiceOver} Script") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        onGenerateVoiceScript(projectTitle) { generated ->
                            voiceScript = generated
                            if (subtitleText.isBlank()) subtitleText = generated.take(90)
                        }
                    }) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Script")
                    }
                    OutlinedButton(
                        onClick = { onSynthesizeVoice(voiceScript) },
                        modifier = Modifier.testTag("editor_synthesize_voice_button")
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.generateVoiceOver)
                    }
                }
                if (synthesizedVoicePath != null) {
                    Spacer(Modifier.height(6.dp))
                    SpecBadge(text = "Voice WAV ready: ${synthesizedVoicePath.substringAfterLast('/')}", accent = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(8.dp))
                Text("Audio Sync Offset: ${audioSyncOffsetMs.toInt()} ms", style = MaterialTheme.typography.labelMedium)
                Slider(value = audioSyncOffsetMs, onValueChange = { audioSyncOffsetMs = it }, valueRange = -1000f..1000f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mute Original Clip Audio", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = muteOriginalAudio, onCheckedChange = { muteOriginalAudio = it })
                }
            }
        }

        // 5. Export CTA + Media3 Transformer Progress + Result
        item {
            GlowActionButton(
                text = strings.exportVideo,
                onClick = {
                    onExportVideo(
                        projectTitle,
                        VideoEditConfig(
                            inputUris = timelineClips.toList(),
                            trimStartMs = (trimStartSec * 1000).toLong(),
                            trimEndMs = (trimEndSec * 1000).toLong(),
                            contrast = contrast,
                            saturation = saturation,
                            brightness = brightness,
                            overlayText = overlayText,
                            subtitleText = subtitleText,
                            includeWatermark = includeWatermark,
                            removeOriginalAudio = muteOriginalAudio,
                            targetWidth = if (export4K) 3840 else 1920,
                            targetHeight = if (export4K) 2160 else 1080,
                            speedMultiplier = speedMultiplier,
                            backgroundAudioPath = bgMusicPath,
                            transitionStyle = selectedTransition
                        )
                    )
                },
                enabled = timelineClips.isNotEmpty() && editorProgress == null,
                modifier = Modifier.fillMaxWidth(),
                testTag = "editor_export_button",
                leadingIcon = { Icon(Icons.Rounded.VideoSettings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (editorProgress != null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Media3 Transformer Exporting MP4...", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { editorProgress / 100f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text("$editorProgress%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (exportedVideoPath != null) {
            item {
                GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Text("Exported Studio Output", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Media3VideoPlayer(mediaUriOrPath = exportedVideoPath)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            onSaveToDevice(exportedVideoPath, "${projectTitle}.mp4")
                        }) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.saveToDevice)
                        }
                        OutlinedButton(onClick = {
                            onCreateShareIntent(exportedVideoPath, "video/mp4", projectTitle)?.let { intent ->
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
    }
}
