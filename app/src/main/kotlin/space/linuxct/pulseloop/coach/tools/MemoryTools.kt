package space.linuxct.pulseloop.coach.tools

import org.json.JSONObject
import space.linuxct.pulseloop.data.db.entities.CoachMemoryEntity
import java.util.UUID

object MemoryTools {

    fun build(): List<CoachTool> = listOf(saveMemory())

    private const val SPEC_SAVE_MEMORY =
        """{"type":"function","name":"save_memory","description":"Persists a piece of information about the user that should be recalled in future conversations (e.g. a preference, a goal, a health note). Use a short snake_case key. Existing entries with the same key are overwritten.","parameters":{"type":"object","properties":{"key":{"type":"string","description":"Short unique identifier for this memory (snake_case, max 64 chars)"},"value":{"type":"string","description":"The information to remember (plain text, max 500 chars)"},"importance":{"type":"integer","description":"Priority 1–5 (5 = most important). Use 3 for general facts, 5 for critical health info."}},"required":["key","value","importance"],"additionalProperties":false},"strict":true}"""

    private fun saveMemory() = CoachTool(
        name = "save_memory",
        specJson = SPEC_SAVE_MEMORY,
        run = { args, ctx ->
            val o          = JSONObject(args)
            val key        = o.optString("key", "").take(64).replace(Regex("[^a-z0-9_]"), "_")
            val value      = o.optString("value", "").take(500)
            val importance = o.optInt("importance", 3).coerceIn(1, 5)

            if (key.isBlank() || value.isBlank()) {
                return@CoachTool ToolResult.error("key and value must not be empty")
            }

            val now = System.currentTimeMillis()
            ctx.coachDao.upsertMemory(
                CoachMemoryEntity(
                    id = UUID.randomUUID().toString(),
                    key = key, value = value, importance = importance,
                    createdAt = now, updatedAt = now, expiresAt = null
                )
            )

            ToolResult.success(JSONObject().put("saved", true).put("key", key))
        }
    )
}
