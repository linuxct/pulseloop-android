package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.dao.CoachDao
import space.linuxct.pulseloop.data.db.entities.CoachConversationEntity
import space.linuxct.pulseloop.data.db.entities.CoachMemoryEntity
import space.linuxct.pulseloop.data.db.entities.CoachMessageEntity
import space.linuxct.pulseloop.data.db.entities.CoachNotificationRecordEntity
import space.linuxct.pulseloop.data.db.entities.CoachSummaryEntity
import space.linuxct.pulseloop.data.db.entities.CoachToolCallEntity
import space.linuxct.pulseloop.domain.repository.CoachRepository
import javax.inject.Inject

class CoachRepositoryImpl @Inject constructor(
    private val dao: CoachDao
) : CoachRepository {
    override fun observeConversations(): Flow<List<CoachConversationEntity>> = dao.observeConversations()
    override suspend fun getConversation(id: String) = dao.getConversation(id)
    override suspend fun upsertConversation(conversation: CoachConversationEntity) = dao.upsertConversation(conversation)
    override suspend fun deleteConversation(id: String) = dao.deleteConversation(id)
    override fun observeMessages(conversationId: String): Flow<List<CoachMessageEntity>> = dao.observeMessages(conversationId)
    override suspend fun getRecentMessages(conversationId: String, limit: Int) = dao.getRecentMessages(conversationId, limit)
    override suspend fun insertMessage(message: CoachMessageEntity) = dao.insertMessage(message)
    override suspend fun insertToolCall(call: CoachToolCallEntity) = dao.insertToolCall(call)
    override suspend fun getAllMemories() = dao.getAllMemories()
    override suspend fun upsertMemory(memory: CoachMemoryEntity) = dao.upsertMemory(memory)
    override suspend fun deleteMemory(key: String) = dao.deleteMemory(key)
    override suspend fun isNotificationDuplicate(dateKey: String, slot: String): Boolean =
        dao.notificationCount(dateKey, slot) > 0
    override suspend fun recordNotification(record: CoachNotificationRecordEntity) = dao.recordNotification(record)
    override suspend fun getSummary(kind: String, scopeKey: String) = dao.getSummary(kind, scopeKey)
    override suspend fun upsertSummary(summary: CoachSummaryEntity) = dao.upsertSummary(summary)
}
