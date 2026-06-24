package space.linuxct.pulseloop.diagnostics

import space.linuxct.pulseloop.core.util.newUUID
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity

object WearableLogHelper {
    fun build(category: String, level: String, message: String, detail: String? = null): WearableLogEntity =
        WearableLogEntity(
            id = newUUID(),
            at = System.currentTimeMillis(),
            category = category,
            level = level,
            message = message,
            detail = detail
        )
}
