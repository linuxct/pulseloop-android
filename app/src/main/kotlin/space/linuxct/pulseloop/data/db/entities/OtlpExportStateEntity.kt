package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-data-type cursor for the OTLP exporter.
 *
 * [lastTimestampMs] is a high-water mark by the data's CAPTURE timestamp; the incremental
 * exporter re-scans `[lastTimestampMs - OVERLAP, now]` to catch backdated ring inserts and
 * relies on TSDB same-timestamp dedupe. [backfillCursorMs] is the resumable position of an
 * in-progress historical backfill (0 = none). [exportedCount] backs the completeness guard
 * against `MeasurementDao.countByKind`.
 */
@Entity(tableName = "otlp_export_state")
data class OtlpExportStateEntity(
    @PrimaryKey val dataType: String,
    val lastTimestampMs: Long,
    val backfillCursorMs: Long,
    val exportedCount: Long,
    val lastError: String?,
    val updatedAt: Long,
)
