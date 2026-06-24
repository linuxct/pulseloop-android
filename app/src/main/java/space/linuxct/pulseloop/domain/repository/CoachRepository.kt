package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.CoachConversationEntity
import space.linuxct.pulseloop.data.db.entities.CoachMemoryEntity
import space.linuxct.pulseloop.data.db.entities.CoachMessageEntity
import space.linuxct.pulseloop.data.db.entities.CoachNotificationRecordEntity
import space.linuxct.pulseloop.data.db.entities.CoachSummaryEntity
import space.linuxct.pulseloop.data.db.entities.CoachToolCallEntity

interface CoachRepository {
    fun observeConversations(): Flow<List<CoachConversationEntity>>
    suspend fun getConversation(id: String): CoachConversationEntity?
    suspend fun upsertConversation(conversation: CoachConversationEntity)
    suspend fun deleteConversation(id: String)

    fun observeMessages(conversationId: String): Flow<List<CoachMessageEntity>>
    suspend fun getRecentMessages(conversationId: String, limit: Int = 20): List<CoachMessageEntity>
    suspend fun insertMessage(message: CoachMessageEntity)

    suspend fun insertToolCall(call: CoachToolCallEntity)

    suspend fun getAllMemories(): List<CoachMemoryEntity>
    suspend fun upsertMemory(memory: CoachMemoryEntity)
    suspend fun deleteMemory(key: String)

    suspend fun isNotificationDuplicate(dateKey: String, slot: String): Boolean
    suspend fun recordNotification(record: CoachNotificationRecordEntity)

    suspend fun getSummary(kind: String, scopeKey: String): CoachSummaryEntity?
    suspend fun upsertSummary(summary: CoachSummaryEntity)
}
