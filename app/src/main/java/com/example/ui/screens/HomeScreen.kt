package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProjectEntity
import com.example.data.local.SavedPromptEntity
import com.example.data.local.UserAccountEntity
import com.example.domain.model.SubscriptionPlan
import com.example.localization.LocalAppStrings
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.SpecBadge

enum class StudioDestination {
    HOME, AI_VIDEO, AI_IMAGES, AI_CHAT, AI_EDITOR, PROJECTS, GALLERY, SETTINGS
}

@Composable
fun HomeScreen(
    account: UserAccountEntity?,
    projects: List<ProjectEntity>,
    savedPrompts: List<SavedPromptEntity>,
    onNavigate: (StudioDestination) -> Unit,
    onApplyPromptToVideo: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val plan = SubscriptionPlan.fromId(account?.planId ?: "free")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_studio),
                    contentDescription = "MA AI Studio Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x3306040C), Color(0xE606040C))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecBadge(text = plan.title, accent = MaterialTheme.colorScheme.secondary)
                            SpecBadge(text = "${account?.credits ?: 0} ${strings.creditsLabel}", accent = MaterialTheme.colorScheme.tertiary)
                        }
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE9D5FF)
                        )
                        Spacer(Modifier.height(4.dp))
                        GlowActionButton(
                            text = strings.generateVideo,
                            onClick = { onNavigate(StudioDestination.AI_VIDEO) },
                            testTag = "home_launch_video_button",
                            leadingIcon = { Icon(Icons.Rounded.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        item {
            FirebaseNoticeBanner()
        }

        item {
            Text(
                text = "Studio Workspaces",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            val actions = listOf(
                Triple(StudioDestination.AI_VIDEO, strings.navVideo, Icons.Rounded.MovieCreation),
                Triple(StudioDestination.AI_IMAGES, strings.navImages, Icons.Rounded.Image),
                Triple(StudioDestination.AI_CHAT, strings.navChat, Icons.Rounded.ChatBubble),
                Triple(StudioDestination.AI_EDITOR, strings.navEditor, Icons.Rounded.Tune),
                Triple(StudioDestination.PROJECTS, strings.navProjects, Icons.Rounded.Folder),
                Triple(StudioDestination.GALLERY, strings.navGallery, Icons.Rounded.PhotoLibrary)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (dest, label, icon) ->
                            WorkspaceTile(
                                label = label,
                                icon = icon,
                                onClick = { onNavigate(dest) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_tile_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.recentCreations,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ShowcaseCard(
                        imageRes = R.drawable.img_showcase_trailer,
                        title = "Sci-Fi Metropolis Trailer",
                        specs = "4K • 24 FPS • 16:9",
                        prompt = "Anamorphic 35mm tracking shot through a neon rain metropolis, violet reflections, volumetric atmosphere",
                        onUsePrompt = onApplyPromptToVideo
                    )
                }
                item {
                    ShowcaseCard(
                        imageRes = R.drawable.img_showcase_product,
                        title = "Obsidian Audio Product Ad",
                        specs = "1080p • 60 FPS • 4:5",
                        prompt = "Matte-black luxury headphones floating over obsidian glass with electric purple studio rim light",
                        onUsePrompt = onApplyPromptToVideo
                    )
                }
            }
        }

        if (savedPrompts.isNotEmpty()) {
            item {
                Text(
                    text = strings.savedPrompts,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(savedPrompts.take(4)) { prompt ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyPromptToVideo(prompt.promptText) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prompt.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        SpecBadge(text = prompt.category)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = prompt.promptText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recent Studio Jobs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (projects.isEmpty()) strings.emptyProjects else "${projects.size} saved project(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShowcaseCard(
    imageRes: Int,
    title: String,
    specs: String,
    prompt: String,
    onUsePrompt: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(268.dp)
            .clickable { onUsePrompt(prompt) },
        contentPadding = PaddingValues(12.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecBadge(text = specs)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text("Use", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
