package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.remote.ProviderConfig
import com.example.domain.model.ChatProvider
import com.example.localization.LocalAppStrings
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.SpecBadge
import com.example.ui.components.rememberDocumentPicker
import com.example.ui.components.rememberPhotoPicker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    sessions: List<ChatSessionEntity>,
    activeSessionId: Long?,
    messages: List<ChatMessageEntity>,
    streamingReply: String?,
    onSelectSession: (Long) -> Unit,
    onNewSession: (ChatProvider) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onSendMessage: (String, ChatProvider, String?, String?) -> Unit,
    onRegenerate: (ChatMessageEntity, ChatProvider) -> Unit,
    onImportAttachment: (Uri, String, (String) -> Unit) -> Unit,
    onUseAsVideoPrompt: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current

    var selectedProvider by remember { mutableStateOf(ChatProvider.GEMINI_FLASH) }
    var inputText by remember { mutableStateOf("") }
    var attachmentPath by remember { mutableStateOf<String?>(null) }
    var attachmentName by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberPhotoPicker { uri ->
        onImportAttachment(uri, "PHOTO") { path ->
            attachmentPath = path
            attachmentName = path.substringAfterLast('/')
        }
    }
    val pickDoc = rememberDocumentPicker { uri ->
        onImportAttachment(uri, "DOCUMENT") { path ->
            attachmentPath = path
            attachmentName = path.substringAfterLast('/')
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + New Session + Provider Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.navChat, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = { onNewSession(selectedProvider) },
                modifier = Modifier.testTag("chat_new_session_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.newChat)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatProvider.entries.forEach { provider ->
                val ready = if (provider.requiresFirebaseAiLogic) {
                    ProviderConfig.isFirebaseAiLogicEnabled
                } else {
                    ProviderConfig.isOpenAiConfigured()
                }
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = { selectedProvider = provider },
                    label = { Text("${provider.displayName} ${if (ready) "" else "(Sample/Setup)"}") },
                    modifier = Modifier.testTag("chat_provider_${provider.id}")
                )
            }
        }

        if (selectedProvider == ChatProvider.GEMINI_FLASH) {
            FirebaseNoticeBanner()
        }

        // Session History Chips
        if (sessions.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { session ->
                    FilterChip(
                        selected = session.id == activeSessionId,
                        onClick = { onSelectSession(session.id) },
                        label = { Text(session.title.take(18)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = strings.delete, modifier = Modifier.size(14.dp))
                            }
                        }
                    )
                }
            }
        }

        // Prompt Generator Quick Chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SuggestionChip(
                onClick = { inputText = "Create a detailed 4K cinematic Veo/Runway video prompt for a futuristic electric sports car commercial." },
                label = { Text(strings.videoPromptCreator) }
            )
            SuggestionChip(
                onClick = { inputText = "Generate a luxury studio product photography prompt with volumetric violet rim lighting." },
                label = { Text("Image Prompt") }
            )
            SuggestionChip(
                onClick = { inputText = "Write a punchy 15-second voice-over script and subtitles for a social media reel." },
                label = { Text("Reel Script") }
            )
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                GlassCard(
                    highlightBorder = !isUser,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpecBadge(
                            text = if (isUser) "YOU" else selectedProvider.displayName.uppercase(),
                            accent = if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        if (msg.isSampleNotice) {
                            SpecBadge(text = "SAMPLE (FIREBASE REQUIRED)", accent = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    if (!msg.attachmentName.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Attachment: ${msg.attachmentName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(msg.content)) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = strings.copyText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.copyText)
                        }
                        if (isUser) {
                            TextButton(onClick = { inputText = msg.content }) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.editPrompt, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.editPrompt)
                            }
                            TextButton(onClick = { onRegenerate(msg, selectedProvider) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = strings.regenerate, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.regenerate)
                            }
                        } else {
                            TextButton(onClick = { onUseAsVideoPrompt(msg.content) }) {
                                Icon(Icons.Rounded.MovieCreation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Send to Video")
                            }
                        }
                    }
                }
            }

            if (streamingReply != null) {
                item {
                    GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        SpecBadge(text = "STREAMING...", accent = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = streamingReply.ifBlank { "Thinking..." },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Attachment Chip
        if (attachmentName != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(attachmentName!!, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { attachmentName = null; attachmentPath = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear attachment", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Composer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = pickPhoto, modifier = Modifier.testTag("chat_pick_photo_button")) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = strings.uploadReference)
            }
            IconButton(onClick = pickDoc, modifier = Modifier.testTag("chat_pick_doc_button")) {
                Icon(Icons.Rounded.AttachFile, contentDescription = strings.uploadDocument)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(strings.chatPlaceholder) },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            IconButton(
                onClick = {
                    val toSend = inputText.trim()
                    if (toSend.isNotEmpty()) {
                        inputText = ""
                        val path = attachmentPath
                        val name = attachmentName
                        attachmentPath = null
                        attachmentName = null
                        onSendMessage(toSend, selectedProvider, path, name)
                    }
                },
                enabled = streamingReply == null,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
