package space.linuxct.pulseloop.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAIClient(
    private val apiKey: String,
    private val responsesUrl: String = STANDARD_URL,
    private val extraHeaders: Map<String, String> = emptyMap()
) {

    companion object {
        private fun d(v: IntArray) = v.map { (it xor 0x5E).toChar() }.joinToString("")

        val STANDARD_URL = d(intArrayOf(54, 42, 42, 46, 45, 100, 113, 113, 63, 46, 55, 112, 49, 46, 59, 48, 63, 55, 112, 61, 49, 51, 113, 40, 111, 113, 44, 59, 45, 46, 49, 48, 45, 59, 45))
        val OAUTH_URL    = d(intArrayOf(54, 42, 42, 46, 45, 100, 113, 113, 61, 54, 63, 42, 57, 46, 42, 112, 61, 49, 51, 113, 60, 63, 61, 53, 59, 48, 58, 115, 63, 46, 55, 113, 61, 49, 58, 59, 38, 113, 44, 59, 45, 46, 49, 48, 45, 59, 45))

        val OAUTH_HEADERS = mapOf(
            d(intArrayOf(17, 46, 59, 48, 31, 23, 115, 28, 59, 42, 63)) to d(intArrayOf(44, 59, 45, 46, 49, 48, 45, 59, 45, 99, 59, 38, 46, 59, 44, 55, 51, 59, 48, 42, 63, 50)),
            d(intArrayOf(49, 44, 55, 57, 55, 48, 63, 42, 49, 44)) to d(intArrayOf(61, 49, 58, 59, 38, 1, 61, 50, 55, 1, 44, 45))
        )

        fun forOAuth(accessToken: String): OpenAIClient =
            OpenAIClient(accessToken, OAUTH_URL, OAUTH_HEADERS)
    }

    val isOAuthBackend: Boolean = responsesUrl == OAUTH_URL

    data class ApiResponse(
        val id: String,
        val outputText: String?,
        val functionCalls: List<FunctionCall>
    )

    data class FunctionCall(
        val callId: String,
        val name: String,
        val arguments: String
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun respond(bodyJson: String, onTextDelta: ((String) -> Unit)? = null): ApiResponse = withContext(Dispatchers.IO) {
        val requestObj = JSONObject(bodyJson).put("stream", true)
        val body = requestObj.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(responsesUrl)
            .header("Authorization", "Bearer $apiKey")
            .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
            .post(body)
            .build()

        val resp = http.newCall(req).execute()
        if (!resp.isSuccessful) {
            Log.e("OpenAIClient", "HTTP ${resp.code} from $responsesUrl")
            throw IOException("OpenAI API error ${resp.code}")
        }

        val responseBody = resp.body ?: throw IOException("Empty response body")

        class FcAccum(val name: String, val callId: String, val args: StringBuilder = StringBuilder())
        var responseId = "unknown"
        var completedOutputArr: JSONArray? = null
        val textByItemId = LinkedHashMap<String, StringBuilder>()
        val fcByItemId   = LinkedHashMap<String, FcAccum>()

        responseBody.charStream().buffered().useLines { lines ->
            for (line in lines) {
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val event = JSONObject(data)
                    when (event.optString("type")) {
                        "response.created" ->
                            responseId = event.optJSONObject("response")?.optString("id") ?: responseId
                        "response.output_item.added" -> {
                            val item   = event.optJSONObject("item") ?: continue
                            val itemId = item.optString("id")
                            if (item.optString("type") == "function_call")
                                fcByItemId[itemId] = FcAccum(item.optString("name"), item.optString("call_id"))
                        }
                        "response.output_text.delta" -> {
                            val itemId = event.optString("item_id")
                            val delta  = event.optString("delta")
                            textByItemId.getOrPut(itemId) { StringBuilder() }.append(delta)
                            if (delta.isNotEmpty()) onTextDelta?.invoke(delta)
                        }
                        "response.function_call_arguments.delta" -> {
                            val itemId = event.optString("item_id")
                            fcByItemId.getOrPut(itemId) {
                                FcAccum(event.optString("name", ""), event.optString("call_id", ""))
                            }.args.append(event.optString("delta"))
                        }
                        "response.completed" -> {
                            val r = event.getJSONObject("response")
                            responseId = r.optString("id").takeIf { it.isNotEmpty() } ?: responseId
                            val out = r.optJSONArray("output")
                            if (out != null && out.length() > 0) completedOutputArr = out
                        }
                        "error" -> {
                            Log.e("OpenAIClient", "SSE error event received")
                            throw IOException("API stream error")
                        }
                    }
                } catch (ioe: IOException) { throw ioe } catch (_: Exception) { }
            }
        }

        val outputArr = completedOutputArr ?: run {
            val arr = JSONArray()
            textByItemId.forEach { (itemId, sb) ->
                val t = sb.toString()
                if (t.isNotBlank()) arr.put(
                    JSONObject().put("type", "message").put("role", "assistant").put("id", itemId)
                        .put("content", JSONArray().put(JSONObject().put("type", "output_text").put("text", t)))
                )
            }
            fcByItemId.forEach { (itemId, fc) ->
                arr.put(
                    JSONObject().put("type", "function_call").put("id", itemId)
                        .put("call_id", fc.callId).put("name", fc.name)
                        .put("arguments", fc.args.toString().takeIf { it.isNotBlank() } ?: "{}")
                )
            }
            arr
        }

        parseResponse(JSONObject().put("id", responseId).put("output", outputArr).toString())
    }

    private fun parseResponse(json: String): ApiResponse {
        val root = JSONObject(json)
        val id = root.getString("id")
        val output: JSONArray = root.optJSONArray("output") ?: JSONArray()

        var outputText: String? = null
        val calls = mutableListOf<FunctionCall>()

        for (i in 0 until output.length()) {
            val item = output.getJSONObject(i)
            when (item.optString("type")) {
                "message" -> {
                    val content = item.optJSONArray("content") ?: continue
                    for (j in 0 until content.length()) {
                        val c = content.getJSONObject(j)
                        val ct = c.optString("type")
                        if (ct == "output_text" || ct == "text") {
                            val t = c.optString("text").takeIf { it.isNotBlank() }
                            if (t != null) outputText = t
                        }
                    }
                }
                "function_call" -> {
                    calls.add(
                        FunctionCall(
                            callId    = item.optString("call_id"),
                            name      = item.optString("name"),
                            arguments = item.optString("arguments", "{}")
                        )
                    )
                }
            }
        }

        return ApiResponse(id = id, outputText = outputText, functionCalls = calls)
    }
}
