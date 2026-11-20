package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CreditTransactionEntity
import com.example.data.local.StudioSettingsSnapshot
import com.example.data.local.UserAccountEntity
import com.example.data.remote.ProviderConfig
import com.example.domain.model.SubscriptionPlan
import com.example.localization.AppLanguage
import com.example.localization.LocalAppStrings
import com.example.ui.components.GlassCard
import com.example.ui.components.SpecBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    account: UserAccountEntity?,
    settings: StudioSettingsSnapshot,
    transactions: List<CreditTransactionEntity>,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleWatermark: (Boolean) -> Unit,
    onTogglePrivacy: (Boolean) -> Unit,
    onUpdateOpenAiEndpoint: (String, String) -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentPlan = SubscriptionPlan.fromId(account?.planId ?: "free")
    var openAiUrl by remember(settings.openAiBaseUrl) { mutableStateOf(settings.openAiBaseUrl) }
    var openAiModel by remember(settings.openAiModel) { mutableStateOf(settings.openAiModel) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navSettings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // Account Profile
        item {
            GlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_creator_avatar),
                        contentDescription = "Creator Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account?.displayName ?: "Creator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(account?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SpecBadge(text = currentPlan.title)
                            SpecBadge(text = "${account?.credits ?: 0} ${strings.creditsLabel}", accent = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    OutlinedButton(onClick = onSignOut, modifier = Modifier.testTag("settings_signout_button")) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.signOut)
                    }
                }
            }
        }

        // Appearance & Language (6 Languages)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.darkMode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = settings.darkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.language == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.nativeName) },
                            modifier = Modifier.testTag("settings_lang_${lang.code}")
                        )
                    }
                }
            }
        }

        // Subscription Plans
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(strings.subscriptionPlans, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubscriptionPlan.entries.forEach { plan ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${plan.title} • ${plan.priceUsd}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("+${plan.monthlyCredits} credits • Max ${plan.maxResolution}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (currentPlan == plan) {
                                SpecBadge(text = "ACTIVE")
                            } else {
                                OutlinedButton(
                                    onClick = { onSelectPlan(plan) },
                                    modifier = Modifier.testTag("settings_plan_${plan.id}")
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Provider Connections & OpenAI-Compatible Endpoint
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.providerKeysStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ProviderConfig.allStatuses().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(status.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(status.configMethod, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SpecBadge(
                            text = if (status.isConfigured) "ACTIVE" else "SETUP",
                            accent = if (status.isConfigured) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = openAiUrl,
                    onValueChange = { openAiUrl = it },
                    label = { Text("OpenAI-Compatible Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = openAiModel,
                    onValueChange = { openAiModel = it },
                    label = { Text("OpenAI-Compatible Chat Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onUpdateOpenAiEndpoint(openAiUrl, openAiModel) }) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Endpoint")
                }
            }
        }

        // Watermark & Privacy Controls
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Privacy & Export Defaults", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Default Studio Watermark", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = settings.watermarkEnabled, onCheckedChange = onToggleWatermark)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Strict Local-Only Privacy Mode", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = settings.privacyAnalyticsDisabled, onCheckedChange = onTogglePrivacy)
                }
            }
        }

        // Credit Usage Ledger
        if (transactions.isNotEmpty()) {
            item {
                Text(strings.usageHistory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(transactions.take(15), key = { it.id }) { tx ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Balance: ${tx.balanceAfter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SpecBadge(
                            text = if (tx.deltaCredits >= 0) "+${tx.deltaCredits}" else "${tx.deltaCredits}",
                            accent = if (tx.deltaCredits >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
