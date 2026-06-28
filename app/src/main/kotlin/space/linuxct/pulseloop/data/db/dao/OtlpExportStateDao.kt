package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import space.linuxct.pulseloop.data.db.entities.OtlpExportStateEntity

@Dao
interface OtlpExportStateDao {

    @Query("SELECT * FROM otlp_export_state WHERE dataType = :dataType LIMIT 1")
    suspend fun getOne(dataType: String): OtlpExportStateEntity?

    @Query("SELECT * FROM otlp_export_state")
    suspend fun getAll(): List<OtlpExportStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: OtlpExportStateEntity)

    @Query("DELETE FROM otlp_export_state")
    suspend fun clearAll()
}
