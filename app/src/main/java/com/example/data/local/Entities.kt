package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerEmail: String,
    val title: String,
    val type: String, // VIDEO, IMAGE, EDIT
    val mode: String,
    val provider: String,
    val prompt: String,
    val status: String, // QUEUED, PROCESSING, COMPLETED, FAILED, SETUP_REQUIRED
    val progressPercent: Int = 0,
    val resolution: String,
    val fps: Int,
    val aspectRatio: String,
    val durationSec: Int,
    val enhancementPipelineUsed: Boolean = false,
    val localFilePath: String? = null,
    val remoteUrl: String? = null,
    val errorMessage: String? = null,
    val creditsCharged: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerEmail: String,
    val title: String,
    val providerId: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // user, assistant
    val content: String,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val isSampleNotice: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_prompts")
data class SavedPromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerEmail: String,
    val title: String,
    val category: String,
    val promptText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "uploaded_media")
data class UploadedMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerEmail: String,
    val displayName: String,
    val mediaType: String, // PHOTO, VIDEO, AUDIO, DOCUMENT, GENERATED_IMAGE, GENERATED_VIDEO
    val mimeType: String,
    val localPath: String,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "credit_transactions")
data class CreditTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerEmail: String,
    val description: String,
    val deltaCredits: Int,
    val balanceAfter: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val passwordHash: String,
    val planId: String = "free",
    val credits: Int = 50,
    val createdAt: Long = System.currentTimeMillis()
)
