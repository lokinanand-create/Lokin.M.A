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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.remote.ProviderConfig
import com.example.domain.model.CreditCalculator
import com.example.domain.model.ImageProvider
import com.example.domain.model.ImageStudioMode
import com.example.localization.LocalAppStrings
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.SpecBadge
import com.example.ui.components.rememberPhotoPicker
import com.example.ui.viewmodel.GenerationStatus
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiImagesScreen(
    imageGenStatus: GenerationStatus,
    onGenerateImage: (String, ImageStudioMode, ImageProvider, Boolean, Int) -> Unit,
    onImportReference: (Uri, String, (String) -> Unit) -> Unit,
    onSaveToDevice: (String, String) -> Unit,
    onCreateShareIntent: (String, String, String) -> Intent?
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var prompt by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(ImageStudioMode.TEXT_TO_IMAGE) }
    var selectedProvider by remember { mutableStateOf(ImageProvider.OPENAI_IMAGE) }
    var export4K by remember { mutableStateOf(false) }
    var referencePath by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberPhotoPicker { uri ->
        onImportReference(uri, "PHOTO") { path -> referencePath = path }
    }

    val cost = CreditCalculator.imageCost(selectedMode, export4K)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navImages, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        if (selectedProvider.requiresFirebaseAiLogic) {
            item { FirebaseNoticeBanner() }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.selectMode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ImageStudioMode.entries) { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("image_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        item {
            GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Image Studio Prompt") },
                    placeholder = { Text(strings.promptHintImage) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_prompt_input")
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = pickPhoto,
                        modifier = Modifier.testTag("image_upload_ref_button")
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(referencePath?.substringAfterLast('/') ?: strings.uploadReference)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("4K Export", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = export4K,
                            onCheckedChange = { export4K = it },
                            modifier = Modifier.testTag("image_4k_switch")
                        )
                    }
                }

                if (referencePath != null) {
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = File(referencePath!!),
                        contentDescription = "Reference image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.selectProvider, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageProvider.entries.forEach { provider ->
                        val ready = if (provider.requiresFirebaseAiLogic) {
                            ProviderConfig.isFirebaseAiLogicEnabled
                        } else {
                            ProviderConfig.isOpenAiConfigured()
                        }
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = {
                                Text("${provider.displayName} ${if (ready) "(Ready)" else "(Setup)"}")
                            },
                            modifier = Modifier.testTag("image_provider_${provider.id}")
                        )
                    }
                }
            }
        }

        item {
            GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Studio Cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SpecBadge(text = "$cost ${strings.creditsLabel}", accent = MaterialTheme.colorScheme.tertiary)
                    }
                    GlowActionButton(
                        text = strings.generateImage,
                        onClick = { onGenerateImage(prompt, selectedMode, selectedProvider, export4K, cost) },
                        enabled = imageGenStatus !is GenerationStatus.Running,
                        testTag = "generate_image_button",
                        leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        item {
            when (imageGenStatus) {
                is GenerationStatus.Running -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(imageGenStatus.label, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                is GenerationStatus.Completed -> {
                    val project = imageGenStatus.project
                    val modelTarget: Any = project.localFilePath?.let { File(it) } ?: project.remoteUrl.orEmpty()
                    GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = modelTarget,
                            contentDescription = project.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                        Spacer(Modifier.height(10.dp))
                        val saveTarget = project.localFilePath ?: project.remoteUrl.orEmpty()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (saveTarget.isNotBlank()) {
                                OutlinedButton(onClick = {
                                    onSaveToDevice(saveTarget, "${project.title}.png")
                                }) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.saveToDevice)
                                }
                            }
                            OutlinedButton(onClick = {
                                val target = project.localFilePath ?: project.remoteUrl.orEmpty()
                                onCreateShareIntent(target, "image/png", project.title)?.let { intent ->
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
                is GenerationStatus.Error -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(imageGenStatus.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                GenerationStatus.Idle -> Unit
            }
        }
    }
}
