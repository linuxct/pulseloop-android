package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import space.linuxct.pulseloop.domain.model.DecodeConfidence
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.MeasurementSource

interface MeasurementRepository {
    suspend fun insert(
        kind: MeasurementKind,
        value: Double,
        unit: String,
        timestamp: Long,
        source: MeasurementSource,
        confidence: DecodeConfidence,
        activitySessionId: String? = null
    ): String

    suspend fun getByKind(kind: MeasurementKind): List<MeasurementEntity>
    suspend fun getByKindSince(kind: MeasurementKind, cutoffMs: Long): List<MeasurementEntity>
    suspend fun getLatest(kind: MeasurementKind): MeasurementEntity?
    fun observeLatest(kind: MeasurementKind): Flow<MeasurementEntity?>
    fun observeSince(cutoffMs: Long): Flow<List<MeasurementEntity>>
    suspend fun getBySession(sessionId: String): List<MeasurementEntity>
}
