package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE ownerEmail = :email ORDER BY createdAt DESC")
    fun observeProjects(email: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE ownerEmail = :email ORDER BY updatedAt DESC")
    fun observeSessions(email: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String, updatedAt: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND id > :afterMessageId")
    suspend fun deleteMessagesAfter(sessionId: Long, afterMessageId: Long)
}

@Dao
interface PromptDao {
    @Query("SELECT * FROM saved_prompts WHERE ownerEmail = :email ORDER BY createdAt DESC")
    fun observePrompts(email: String): Flow<List<SavedPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: SavedPromptEntity): Long

    @Query("DELETE FROM saved_prompts WHERE id = :id")
    suspend fun deletePrompt(id: Long)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM uploaded_media WHERE ownerEmail = :email ORDER BY createdAt DESC")
    fun observeMedia(email: String): Flow<List<UploadedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: UploadedMediaEntity): Long

    @Update
    suspend fun updateMedia(media: UploadedMediaEntity)

    @Query("DELETE FROM uploaded_media WHERE id = :id")
    suspend fun deleteMedia(id: Long)
}

@Dao
interface CreditDao {
    @Query("SELECT * FROM credit_transactions WHERE ownerEmail = :email ORDER BY createdAt DESC")
    fun observeTransactions(email: String): Flow<List<CreditTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: CreditTransactionEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccount(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    fun observeAccount(email: String): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: UserAccountEntity)
}
