package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.core.util.newUUID
import space.linuxct.pulseloop.data.db.dao.MeasurementDao
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import space.linuxct.pulseloop.domain.model.DecodeConfidence
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.MeasurementSource
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import javax.inject.Inject

class MeasurementRepositoryImpl @Inject constructor(
    private val dao: MeasurementDao
) : MeasurementRepository {

    override suspend fun insert(
        kind: MeasurementKind,
        value: Double,
        unit: String,
        timestamp: Long,
        source: MeasurementSource,
        confidence: DecodeConfidence,
        activitySessionId: String?
    ): String {
        val id = newUUID()
        dao.insert(
            MeasurementEntity(
                id = id,
                kindRaw = kind.rawValue,
                value = value,
                unit = unit,
                timestamp = timestamp,
                sourceRaw = source.rawValue,
                confidenceRaw = confidence.rawValue,
                activitySessionId = activitySessionId
            )
        )
        return id
    }

    override suspend fun getByKind(kind: MeasurementKind) =
        dao.getByKind(kind.rawValue)

    override suspend fun getByKindSince(kind: MeasurementKind, cutoffMs: Long) =
        dao.getByKindSince(kind.rawValue, cutoffMs)

    override suspend fun getLatest(kind: MeasurementKind) =
        dao.getLatest(kind.rawValue)

    override fun observeLatest(kind: MeasurementKind): Flow<MeasurementEntity?> =
        dao.observeLatest(kind.rawValue)

    override fun observeSince(cutoffMs: Long): Flow<List<MeasurementEntity>> =
        dao.observeSince(cutoffMs)

    override suspend fun getBySession(sessionId: String) =
        dao.getBySession(sessionId)
}
