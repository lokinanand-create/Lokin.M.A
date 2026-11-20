package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.localization.AppLanguage
import com.example.localization.LocalAppStrings
import com.example.localization.stringsFor
import com.example.ui.components.CreditConfirmationDialog
import com.example.ui.components.ProviderSetupDialog
import com.example.ui.components.SpecBadge
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AiEditorScreen
import com.example.ui.screens.AiImagesScreen
import com.example.ui.screens.AiVideoScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioDestination
import com.example.ui.theme.MaAiStudioTheme
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object AiVideoRoute
@Serializable data object AiImagesRoute
@Serializable data object AiChatRoute
@Serializable data object AiEditorRoute
@Serializable data object ProjectsRoute
@Serializable data object GalleryRoute
@Serializable data object SettingsRoute

private data class NavItemSpec(
    val dest: StudioDestination,
    val route: Any,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaAiStudioApp(viewModel: StudioViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val account by viewModel.currentAccount.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val savedPrompts by viewModel.savedPrompts.collectAsStateWithLifecycle()
    val uploadedMedia by viewModel.uploadedMedia.collectAsStateWithLifecycle()
    val transactions by viewModel.creditTransactions.collectAsStateWithLifecycle()
    val sessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val videoGenStatus by viewModel.videoGenStatus.collectAsStateWithLifecycle()
    val imageGenStatus by viewModel.imageGenStatus.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val editorProgress by viewModel.editorProgress.collectAsStateWithLifecycle()
    val exportedVideoPath by viewModel.exportedVideoPath.collectAsStateWithLifecycle()
    val synthesizedVoicePath by viewModel.synthesizedVoicePath.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val setupAlert by viewModel.setupAlert.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    val strings = remember(settings.language) { stringsFor(settings.language) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    MaAiStudioTheme(darkTheme = settings.darkMode) {
        CompositionLocalProvider(LocalAppStrings provides strings) {
            if (settings.signedInEmail.isNullOrBlank()) {
                AuthScreen(
                    authError = authError,
                    onSignIn = viewModel::signIn,
                    onRegister = viewModel::register,
                    onContinueGuest = viewModel::continueAsGuest
                )
            } else {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                var pendingVideoPrompt by remember { mutableStateOf("") }
                var pendingEditorClipUri by remember { mutableStateOf<String?>(null) }
                var langMenuExpanded by remember { mutableStateOf(false) }

                val allNavItems = listOf(
                    NavItemSpec(StudioDestination.HOME, HomeRoute, strings.navHome, Icons.Rounded.Home),
                    NavItemSpec(StudioDestination.AI_VIDEO, AiVideoRoute, strings.navVideo, Icons.Rounded.MovieCreation),
                    NavItemSpec(StudioDestination.AI_IMAGES, AiImagesRoute, strings.navImages, Icons.Rounded.Image),
                    NavItemSpec(StudioDestination.AI_CHAT, AiChatRoute, strings.navChat, Icons.Rounded.ChatBubble),
                    NavItemSpec(StudioDestination.AI_EDITOR, AiEditorRoute, strings.navEditor, Icons.Rounded.Tune),
                    NavItemSpec(StudioDestination.PROJECTS, ProjectsRoute, strings.navProjects, Icons.Rounded.Folder),
                    NavItemSpec(StudioDestination.GALLERY, GalleryRoute, strings.navGallery, Icons.Rounded.PhotoLibrary),
                    NavItemSpec(StudioDestination.SETTINGS, SettingsRoute, strings.navSettings, Icons.Rounded.Settings)
                )

                val navigateTo: (StudioDestination) -> Unit = { dest ->
                    val targetRoute = allNavItems.first { it.dest == dest }.route
                    navController.navigate(targetRoute) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    val isWideScreen = maxWidth >= 720.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isWideScreen) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                allNavItems.forEach { item ->
                                    val selected = currentDestination?.hasRoute(item.route::class) == true
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(item.dest) },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label, maxLines = 1) },
                                        modifier = Modifier.testTag("rail_${item.dest.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ),
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(strings.appTitle, fontWeight = FontWeight.Bold)
                                            SpecBadge(text = "${account?.credits ?: 0} ${strings.creditsLabel}")
                                        }
                                    },
                                    actions = {
                                        Box {
                                            IconButton(
                                                onClick = { langMenuExpanded = true },
                                                modifier = Modifier.testTag("topbar_language_button")
                                            ) {
                                                Icon(Icons.Rounded.Language, contentDescription = strings.languageLabel)
                                            }
                                            DropdownMenu(
                                                expanded = langMenuExpanded,
                                                onDismissRequest = { langMenuExpanded = false }
                                            ) {
                                                AppLanguage.entries.forEach { lang ->
                                                    DropdownMenuItem(
                                                        text = { Text(lang.nativeName) },
                                                        onClick = {
                                                            langMenuExpanded = false
                                                            viewModel.setLanguage(lang)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        if (!isWideScreen) {
                                            IconButton(
                                                onClick = { navigateTo(StudioDestination.PROJECTS) },
                                                modifier = Modifier.testTag("topbar_projects_button")
                                            ) {
                                                Icon(Icons.Rounded.Folder, contentDescription = strings.navProjects)
                                            }
                                            IconButton(
                                                onClick = { navigateTo(StudioDestination.GALLERY) },
                                                modifier = Modifier.testTag("topbar_gallery_button")
                                            ) {
                                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = strings.navGallery)
                                            }
                                            IconButton(
                                                onClick = { navigateTo(StudioDestination.SETTINGS) },
                                                modifier = Modifier.testTag("topbar_settings_button")
                                            ) {
                                                Icon(Icons.Rounded.Settings, contentDescription = strings.navSettings)
                                            }
                                        }
                                    }
                                )
                            },
                            bottomBar = {
                                if (!isWideScreen) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                                    ) {
                                        allNavItems.take(5).forEach { item ->
                                            val selected = currentDestination?.hasRoute(item.route::class) == true
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTo(item.dest) },
                                                icon = { Icon(item.icon, contentDescription = item.label) },
                                                label = { Text(item.label, maxLines = 1) },
                                                modifier = Modifier.testTag("bottom_nav_${item.dest.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = HomeRoute,
                                    modifier = Modifier
                                        .widthIn(max = 900.dp)
                                        .fillMaxSize()
                                ) {
                                    composable<HomeRoute> {
                                        HomeScreen(
                                            account = account,
                                            projects = projects,
                                            savedPrompts = savedPrompts,
                                            onNavigate = navigateTo,
                                            onApplyPromptToVideo = { promptText ->
                                                pendingVideoPrompt = promptText
                                                navigateTo(StudioDestination.AI_VIDEO)
                                            }
                                        )
                                    }
                                    composable<AiVideoRoute> {
                                        AiVideoScreen(
                                            initialPrompt = pendingVideoPrompt,
                                            generationStatus = videoGenStatus,
                                            onGenerateVideo = viewModel::requestVideoGeneration,
                                            onEnhancePrompt = viewModel::enhancePrompt,
                                            onSavePrompt = viewModel::savePrompt,
                                            onImportReference = { uri, category, onDone ->
                                                viewModel.importMedia(uri, category) { onDone(it.localPath) }
                                            },
                                            onSaveToDevice = { path, name ->
                                                viewModel.saveToDeviceGallery(path, isVideo = true, displayName = name)
                                            },
                                            onSendToEditor = { videoTarget ->
                                                pendingEditorClipUri = videoTarget
                                                navigateTo(StudioDestination.AI_EDITOR)
                                            },
                                            onCreateShareIntent = viewModel::createShareIntent
                                        )
                                    }
                                    composable<AiImagesRoute> {
                                        AiImagesScreen(
                                            imageGenStatus = imageGenStatus,
                                            onGenerateImage = viewModel::requestImageGeneration,
                                            onImportReference = { uri, category, onDone ->
                                                viewModel.importMedia(uri, category) { onDone(it.localPath) }
                                            },
                                            onSaveToDevice = { path, name ->
                                                viewModel.saveToDeviceGallery(path, isVideo = false, displayName = name)
                                            },
                                            onCreateShareIntent = viewModel::createShareIntent
                                        )
                                    }
                                    composable<AiChatRoute> {
                                        AiChatScreen(
                                            sessions = sessions,
                                            activeSessionId = activeSessionId,
                                            messages = activeMessages,
                                            streamingReply = streamingReply,
                                            onSelectSession = viewModel::selectChatSession,
                                            onNewSession = viewModel::startNewChatSession,
                                            onDeleteSession = viewModel::deleteChatSession,
                                            onSendMessage = viewModel::sendChatMessage,
                                            onRegenerate = viewModel::regenerateLastResponse,
                                            onImportAttachment = { uri, cat, onDone ->
                                                viewModel.importMedia(uri, cat) { onDone(it.localPath) }
                                            },
                                            onUseAsVideoPrompt = { promptText ->
                                                pendingVideoPrompt = promptText
                                                navigateTo(StudioDestination.AI_VIDEO)
                                            }
                                        )
                                    }
                                    composable<AiEditorRoute> {
                                        AiEditorScreen(
                                            initialVideoUri = pendingEditorClipUri,
                                            editorProgress = editorProgress,
                                            exportedVideoPath = exportedVideoPath,
                                            synthesizedVoicePath = synthesizedVoicePath,
                                            defaultWatermark = settings.watermarkEnabled,
                                            onImportMedia = { uri, cat, onDone ->
                                                viewModel.importMedia(uri, cat) { onDone(it.localPath) }
                                            },
                                            onExportVideo = viewModel::exportEditedVideo,
                                            onGenerateVoiceScript = viewModel::generateVoiceOverScript,
                                            onSynthesizeVoice = viewModel::synthesizeVoiceOver,
                                            onSaveToDevice = { path, name ->
                                                viewModel.saveToDeviceGallery(path, isVideo = true, displayName = name)
                                            },
                                            onCreateShareIntent = viewModel::createShareIntent
                                        )
                                    }
                                    composable<ProjectsRoute> {
                                        MyProjectsScreen(
                                            projects = projects,
                                            savedPrompts = savedPrompts,
                                            onDeleteProject = viewModel::deleteProject,
                                            onDeletePrompt = viewModel::deletePrompt,
                                            onSendToEditor = { target ->
                                                pendingEditorClipUri = target
                                                navigateTo(StudioDestination.AI_EDITOR)
                                            },
                                            onUsePrompt = { promptText ->
                                                pendingVideoPrompt = promptText
                                                navigateTo(StudioDestination.AI_VIDEO)
                                            },
                                            onSaveToDevice = viewModel::saveToDeviceGallery,
                                            onCreateShareIntent = viewModel::createShareIntent
                                        )
                                    }
                                    composable<GalleryRoute> {
                                        GalleryScreen(
                                            mediaItems = uploadedMedia,
                                            onImportMedia = { uri, cat -> viewModel.importMedia(uri, cat) },
                                            onReplaceMedia = viewModel::replaceMedia,
                                            onDeleteMedia = viewModel::deleteMedia,
                                            onSaveToDevice = viewModel::saveToDeviceGallery,
                                            onSendVideoToEditor = { target ->
                                                pendingEditorClipUri = target
                                                navigateTo(StudioDestination.AI_EDITOR)
                                            }
                                        )
                                    }
                                    composable<SettingsRoute> {
                                        SettingsScreen(
                                            account = account,
                                            settings = settings,
                                            transactions = transactions,
                                            onToggleDarkMode = viewModel::setDarkMode,
                                            onSelectLanguage = viewModel::setLanguage,
                                            onToggleWatermark = viewModel::setWatermarkEnabled,
                                            onTogglePrivacy = viewModel::setPrivacyMode,
                                            onUpdateOpenAiEndpoint = viewModel::updateOpenAiEndpoint,
                                            onSelectPlan = viewModel::requestPlanUpgrade,
                                            onSignOut = viewModel::signOut
                                        )
                                    }
                                }
                            }
                        }
                    }

                    pendingConfirmation?.let { confirmation ->
                        CreditConfirmationDialog(
                            confirmation = confirmation,
                            onDismiss = viewModel::dismissConfirmation
                        )
                    }

                    setupAlert?.let { alert ->
                        ProviderSetupDialog(
                            alert = alert,
                            onDismiss = viewModel::dismissSetupAlert
                        )
                    }
                }
            }
        }
    }
}
