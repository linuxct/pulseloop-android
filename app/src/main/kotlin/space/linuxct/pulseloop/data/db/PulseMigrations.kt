package space.linuxct.pulseloop.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // activity_daily: add required source column, default '' for existing rows
        db.execSQL("ALTER TABLE `activity_daily` ADD COLUMN `source` TEXT NOT NULL DEFAULT ''")

        // activity_samples: columns were restructured (recordedAt removed, kindRaw/value/unit/timestamp/sourceRaw added,
        // measurementId relaxed to nullable). SQLite can't drop columns so we recreate the table.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `activity_samples_v2` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `measurementId` TEXT,
                `kindRaw` TEXT NOT NULL,
                `value` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `sourceRaw` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sessionId`) REFERENCES `activity_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        // Preserve what we can: id, sessionId, measurementId survive; recordedAt maps to timestamp.
        // kindRaw / value / unit / sourceRaw have no v1 equivalent — use neutral defaults.
        db.execSQL("""
            INSERT INTO `activity_samples_v2`
                (`id`, `sessionId`, `measurementId`, `kindRaw`, `value`, `unit`, `timestamp`, `sourceRaw`)
            SELECT `id`, `sessionId`, `measurementId`, '', 0.0, '', `recordedAt`, ''
            FROM `activity_samples`
        """.trimIndent())

        db.execSQL("DROP TABLE `activity_samples`")
        db.execSQL("ALTER TABLE `activity_samples_v2` RENAME TO `activity_samples`")

        // Recreate the index that v2 adds on measurementId
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_activity_samples_measurementId` ON `activity_samples` (`measurementId`)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The measurements composite index was changed from non-unique to unique.
        // Deduplicate first (keep the row with the smallest id for each kindRaw+timestamp pair)
        // so the unique constraint can be applied without error.
        db.execSQL("""
            DELETE FROM `measurements`
            WHERE `id` NOT IN (
                SELECT MIN(`id`) FROM `measurements` GROUP BY `kindRaw`, `timestamp`
            )
        """.trimIndent())

        db.execSQL("DROP INDEX IF EXISTS `index_measurements_kindRaw_timestamp`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_measurements_kindRaw_timestamp` ON `measurements` (`kindRaw`, `timestamp`)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // activity_daily: two new columns for delta-based step tracking.
        // Existing rows start at 0 — correct baseline for rings that haven't reset yet.
        db.execSQL(
            "ALTER TABLE `activity_daily` ADD COLUMN `stepBaseline` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `activity_daily` ADD COLUMN `stepsSaved` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
