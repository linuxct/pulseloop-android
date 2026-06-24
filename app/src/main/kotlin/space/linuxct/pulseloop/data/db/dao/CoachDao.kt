package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.CoachConversationEntity
import space.linuxct.pulseloop.data.db.entities.CoachMemoryEntity
import space.linuxct.pulseloop.data.db.entities.CoachMessageEntity
import space.linuxct.pulseloop.data.db.entities.CoachNotificationRecordEntity
import space.linuxct.pulseloop.data.db.entities.CoachSummaryEntity
import space.linuxct.pulseloop.data.db.entities.CoachToolCallEntity

@Dao
interface CoachDao {

    // Conversations
    @Query("SELECT * FROM coach_conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<CoachConversationEntity>>

    @Query("SELECT * FROM coach_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): CoachConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: CoachConversationEntity)

    // Use this when the conversation already exists — avoids INSERT OR REPLACE which
    // CASCADE-deletes all child messages before re-inserting the conversation row.
    @Update
    suspend fun updateConversation(conversation: CoachConversationEntity)

    @Query("DELETE FROM coach_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    // Messages
    @Query("SELECT * FROM coach_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessages(conversationId: String): Flow<List<CoachMessageEntity>>

    @Query("SELECT * FROM coach_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String, limit: Int = 20): List<CoachMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CoachMessageEntity)

    @Query("DELETE FROM coach_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    // Tool calls
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolCall(call: CoachToolCallEntity)

    @Query("SELECT * FROM coach_tool_calls WHERE messageId = :messageId")
    suspend fun getToolCallsForMessage(messageId: String): List<CoachToolCallEntity>

    // Memories
    @Query("SELECT * FROM coach_memories ORDER BY importance DESC, updatedAt DESC")
    suspend fun getAllMemories(): List<CoachMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: CoachMemoryEntity)

    @Query("DELETE FROM coach_memories WHERE key = :key")
    suspend fun deleteMemory(key: String)

    @Query("DELETE FROM coach_memories WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun pruneExpiredMemories(now: Long)

    // Notification dedup
    @Query("SELECT COUNT(*) FROM coach_notification_records WHERE dateKey = :dateKey AND slotRaw = :slot")
    suspend fun notificationCount(dateKey: String, slot: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordNotification(record: CoachNotificationRecordEntity)

    // Summaries
    @Query("SELECT * FROM coach_summaries WHERE kind = :kind AND scopeKey = :scopeKey LIMIT 1")
    suspend fun getSummary(kind: String, scopeKey: String): CoachSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: CoachSummaryEntity)
}
