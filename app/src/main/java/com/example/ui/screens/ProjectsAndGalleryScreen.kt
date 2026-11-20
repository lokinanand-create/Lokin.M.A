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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ProjectEntity
import com.example.data.local.SavedPromptEntity
import com.example.data.local.UploadedMediaEntity
import com.example.localization.LocalAppStrings
import com.example.ui.components.GlassCard
import com.example.ui.components.Media3VideoPlayer
import com.example.ui.components.SpecBadge
import com.example.ui.components.rememberAudioPicker
import com.example.ui.components.rememberDocumentPicker
import com.example.ui.components.rememberPhotoPicker
import com.example.ui.components.rememberVideoPicker
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyProjectsScreen(
    projects: List<ProjectEntity>,
    savedPrompts: List<SavedPromptEntity>,
    onDeleteProject: (Long) -> Unit,
    onDeletePrompt: (Long) -> Unit,
    onSendToEditor: (String) -> Unit,
    onUsePrompt: (String) -> Unit,
    onSaveToDevice: (String, Boolean, String) -> Unit,
    onCreateShareIntent: (String, String, String) -> Intent?
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(projects, filter) {
        if (filter == "ALL") projects else projects.filter { it.type == filter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.navProjects, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "VIDEO", "IMAGE", "EDIT").forEach { key ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(key) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.emptyProjects, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { project ->
                val target = project.localFilePath ?: project.remoteUrl
                GlassCard(highlightBorder = project.status == "COMPLETED", modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        SpecBadge(text = "${project.type} • ${project.resolution}")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${project.provider} • ${project.mode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(4.dp))
                    Text(project.prompt, style = MaterialTheme.typography.bodySmall, maxLines = 2)

                    if (!target.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        if (project.type == "IMAGE") {
                            AsyncImage(
                                model = project.localFilePath?.let { File(it) } ?: project.remoteUrl,
                                contentDescription = project.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        } else {
                            Media3VideoPlayer(mediaUriOrPath = target)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!target.isNullOrBlank()) {
                            OutlinedButton(onClick = {
                                onSaveToDevice(
                                    target,
                                    project.type != "IMAGE",
                                    "${project.title}.${if (project.type == "IMAGE") "png" else "mp4"}"
                                )
                            }) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.saveToDevice)
                            }
                        }
                        if (project.type != "IMAGE" && !target.isNullOrBlank()) {
                            OutlinedButton(onClick = { onSendToEditor(target) }) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.navEditor)
                            }
                        }
                        if (!target.isNullOrBlank()) {
                            OutlinedButton(onClick = {
                                onCreateShareIntent(
                                    target,
                                    if (project.type == "IMAGE") "image/png" else "video/mp4",
                                    project.title
                                )?.let { intent -> context.startActivity(Intent.createChooser(intent, strings.share)) }
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.share)
                            }
                        }
                        OutlinedButton(onClick = { onDeleteProject(project.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.delete)
                        }
                    }
                }
            }
        }

        if (savedPrompts.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(strings.savedPrompts, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(savedPrompts, key = { "prompt_${it.id}" }) { prompt ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(prompt.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        SpecBadge(text = prompt.category)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(prompt.promptText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onUsePrompt(prompt.promptText) }) {
                            Text("Use in Video")
                        }
                        OutlinedButton(onClick = { onDeletePrompt(prompt.id) }) {
                            Text(strings.delete)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    mediaItems: List<UploadedMediaEntity>,
    onImportMedia: (Uri, String) -> Unit,
    onReplaceMedia: (UploadedMediaEntity, Uri) -> Unit,
    onDeleteMedia: (UploadedMediaEntity) -> Unit,
    onSaveToDevice: (String, Boolean, String) -> Unit,
    onSendVideoToEditor: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var pendingReplaceItem by remember { mutableStateOf<UploadedMediaEntity?>(null) }

    val pickPhoto = rememberPhotoPicker { uri -> onImportMedia(uri, "PHOTO") }
    val pickVideo = rememberVideoPicker { uri -> onImportMedia(uri, "VIDEO") }
    val pickAudio = rememberAudioPicker { uri -> onImportMedia(uri, "AUDIO") }
    val pickDoc = rememberDocumentPicker { uri -> onImportMedia(uri, "DOCUMENT") }

    val replacePhotoPicker = rememberPhotoPicker { uri ->
        pendingReplaceItem?.let { onReplaceMedia(it, uri) }
        pendingReplaceItem = null
    }
    val replaceVideoPicker = rememberVideoPicker { uri ->
        pendingReplaceItem?.let { onReplaceMedia(it, uri) }
        pendingReplaceItem = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.navGallery, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Upload & Manage Studio Assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = pickPhoto, modifier = Modifier.testTag("gallery_upload_photo")) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.uploadReference)
                    }
                    OutlinedButton(onClick = pickVideo, modifier = Modifier.testTag("gallery_upload_video")) {
                        Icon(Icons.Rounded.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.uploadVideo)
                    }
                    OutlinedButton(onClick = pickAudio, modifier = Modifier.testTag("gallery_upload_audio")) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.uploadAudio)
                    }
                    OutlinedButton(onClick = pickDoc, modifier = Modifier.testTag("gallery_upload_doc")) {
                        Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.uploadDocument)
                    }
                }
            }
        }

        if (mediaItems.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.emptyGallery, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(mediaItems, key = { it.id }) { item ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        SpecBadge(text = "${item.mediaType} • ${item.sizeBytes / 1024} KB")
                    }
                    Spacer(Modifier.height(8.dp))
                    when (item.mediaType) {
                        "PHOTO", "GENERATED_IMAGE" -> {
                            AsyncImage(
                                model = File(item.localPath),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                        "VIDEO", "GENERATED_VIDEO" -> {
                            Media3VideoPlayer(mediaUriOrPath = item.localPath)
                        }
                        "AUDIO" -> {
                            Media3VideoPlayer(
                                mediaUriOrPath = item.localPath,
                                aspectRatio = 2.4f,
                                modifier = Modifier.height(92.dp)
                            )
                        }
                        else -> {
                            Text(item.mimeType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.mediaType == "VIDEO" || item.mediaType == "GENERATED_VIDEO") {
                            OutlinedButton(onClick = { onSendVideoToEditor(item.localPath) }) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.navEditor)
                            }
                        }
                        if (item.mediaType == "PHOTO" || item.mediaType == "VIDEO") {
                            OutlinedButton(onClick = {
                                pendingReplaceItem = item
                                if (item.mediaType == "PHOTO") replacePhotoPicker() else replaceVideoPicker()
                            }) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.replace)
                            }
                        }
                        if (item.mediaType in listOf("PHOTO", "VIDEO", "GENERATED_IMAGE", "GENERATED_VIDEO")) {
                            val isVid = item.mediaType == "VIDEO" || item.mediaType == "GENERATED_VIDEO"
                            OutlinedButton(onClick = {
                                onSaveToDevice(item.localPath, isVid, item.displayName)
                            }) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.saveToDevice)
                            }
                        }
                        OutlinedButton(onClick = { onDeleteMedia(item) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.delete)
                        }
                    }
                }
            }
        }
    }
}
