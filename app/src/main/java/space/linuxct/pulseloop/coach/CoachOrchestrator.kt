package space.linuxct.pulseloop.coach

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import space.linuxct.pulseloop.coach.model.CoachResponse
import space.linuxct.pulseloop.coach.tools.CoachTool
import space.linuxct.pulseloop.coach.tools.ToolContext
import space.linuxct.pulseloop.coach.tools.ToolResult
import space.linuxct.pulseloop.data.db.entities.CoachMessageEntity
import space.linuxct.pulseloop.data.network.OpenAIClient

class CoachOrchestrator(
    private val client: OpenAIClient,
    private val tools: List<CoachTool>,
    private val toolCtx: ToolContext,
    private val model: String = "gpt-5.4",
    private val maxRounds: Int = 8,
    private val onTrace: (String) -> Unit = {}
) {

    sealed class TurnResult {
        data class Success(
            val response: CoachResponse,
            val rawJson: String,
            val responseId: String
        ) : TurnResult()
        data class Failure(val message: String) : TurnResult()
    }

    suspend fun runTurn(
        systemPrompt: String,
        developerMessage: String,
        userText: String,
        conversationHistory: List<CoachMessageEntity> = emptyList(),
        onTextDelta: ((String) -> Unit)? = null
    ): TurnResult {
        val toolsByName  = tools.associateBy { it.name }
        val instructions = "$systemPrompt\n\n$developerMessage"
        val toolsArr     = buildToolsArray()

        val input = JSONArray()
        for (msg in conversationHistory.takeLast(6)) {
            val role    = if (msg.roleRaw == "user") "user" else "assistant"
            val content = msg.textContent?.takeIf { it.isNotBlank() } ?: continue
            input.put(JSONObject().put("role", role).put("content", content))
        }
        input.put(JSONObject().put("role", "user").put("content", userText))

        onTrace("Thinking…")
        var response = try {
            client.respond(buildRequest(instructions, input, toolsArr), onTextDelta)
        } catch (e: Exception) {
            Log.e("CoachOrchestrator", "Initial API call failed: ${e.message}", e)
            return TurnResult.Failure("Network error: ${e.message}")
        }

        var rounds = 0
        while (response.functionCalls.isNotEmpty() && rounds < maxRounds) {
            rounds++

            for (call in response.functionCalls) {
                input.put(
                    JSONObject()
                        .put("type",      "function_call")
                        .put("call_id",   call.callId)
                        .put("name",      call.name)
                        .put("arguments", call.arguments)
                )
            }

            for (call in response.functionCalls) {
                onTrace("Checking ${call.name.replace('_', ' ')}…")
                val tool = toolsByName[call.name]
                val result: ToolResult = if (tool != null) {
                    try { tool.run(call.arguments, toolCtx) }
                    catch (e: Exception) {
                        Log.e("CoachOrchestrator", "Tool ${call.name} threw: ${e.message}", e)
                        ToolResult.error("${call.name} failed: ${e.message}")
                    }
                } else {
                    Log.w("CoachOrchestrator", "Unknown tool requested: ${call.name}")
                    ToolResult.error("Unknown tool: ${call.name}")
                }
                input.put(
                    JSONObject()
                        .put("type",    "function_call_output")
                        .put("call_id", call.callId)
                        .put("output",  result.json)
                )
            }

            onTrace("Processing results…")
            response = try {
                client.respond(buildRequest(instructions, input, toolsArr), onTextDelta)
            } catch (e: Exception) {
                Log.e("CoachOrchestrator", "Tool-result API call failed (round $rounds): ${e.message}", e)
                return TurnResult.Failure("Network error in tool round $rounds: ${e.message}")
            }
        }

        val text = response.outputText
            ?: run {
                Log.e("CoachOrchestrator", "Model returned no text after $rounds tool round(s). id=${response.id}")
                return TurnResult.Failure("Model returned no text output")
            }

        return try {
            TurnResult.Success(CoachResponse.fromJson(text), text, response.id)
        } catch (e: Exception) {
            val fallback = CoachResponse(
                responseType = "summary", title = "Response",
                summary = text.take(600), bullets = emptyList(),
                chart = null, safetyNote = null, dataQualityNote = null,
                sources = emptyList(), followUpChips = emptyList(),
                actionsTaken = emptyList(), confidence = "medium"
            )
            TurnResult.Success(fallback, text, response.id)
        }
    }

    // ── Request builder ────────────────────────────────────────────────────────

    private fun buildRequest(instructions: String, input: JSONArray, toolsArr: JSONArray): String {
        val req = JSONObject()
            .put("model",        model)
            .put("store",        false)
            .put("instructions", instructions)
            .put("input",        input)
            .put("tools",        toolsArr)

        if (client.isOAuthBackend) {
            req.put("reasoning", JSONObject().put("effort", "medium"))
        } else {
            // Standard OpenAI API: enforce the structured response schema.
            req.put("text", JSONObject()
                .put("format", JSONObject()
                    .put("type",   "json_schema")
                    .put("name",   "coach_response")
                    .put("strict", false)
                    .put("schema", JSONObject(RESPONSE_SCHEMA))
                )
            )
        }

        return req.toString()
    }

    private fun buildToolsArray(): JSONArray {
        val arr = JSONArray()
        tools.forEach { tool ->
            val spec = JSONObject(tool.specJson)
            if (client.isOAuthBackend) spec.remove("strict")
            arr.put(spec)
        }
        return arr
    }

    // ── Response JSON schema (used on the standard OpenAI API) ─────────────────

    private companion object {
        // language=JSON
        const val RESPONSE_SCHEMA = """{
            "type": "object",
            "properties": {
                "response_type": {"type": "string", "enum": ["insight","summary","action_done","clarify","error"]},
                "title":         {"type": "string"},
                "summary":       {"type": "string"},
                "bullets":       {"type": "array",  "items": {"type": "string"}},
                "chart": {
                    "anyOf": [
                        {"type": "null"},
                        {"type": "object",
                         "properties": {
                             "type":  {"type": "string"},
                             "title": {"type": "string"},
                             "unit":  {"type": "string"},
                             "data":  {"type": "array", "items": {
                                 "type": "object",
                                 "properties": {"label": {"type": "string"}, "value": {"type": "number"}},
                                 "required": ["label","value"]
                             }}
                         },
                         "required": ["type","title","unit","data"]}
                    ]
                },
                "safety_note":       {"anyOf": [{"type": "null"}, {"type": "string"}]},
                "data_quality_note": {"anyOf": [{"type": "null"}, {"type": "string"}]},
                "sources": {
                    "type": "array",
                    "items": {"type": "object",
                              "properties": {"tool": {"type": "string"}, "summary": {"type": "string"}},
                              "required": ["tool","summary"]}
                },
                "follow_up_chips": {"type": "array", "items": {"type": "string"}},
                "actions_taken":   {"type": "array", "items": {"type": "string"}},
                "confidence": {"type": "string", "enum": ["high","medium","low","insufficient_data"]}
            },
            "required": [
                "response_type","title","summary","bullets","chart",
                "safety_note","data_quality_note","sources",
                "follow_up_chips","actions_taken","confidence"
            ],
            "additionalProperties": false
        }"""
    }
}
