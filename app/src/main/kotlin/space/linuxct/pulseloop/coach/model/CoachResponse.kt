package space.linuxct.pulseloop.coach.model

import org.json.JSONObject

data class CoachResponse(
    val responseType: String,
    val title: String,
    val summary: String,
    val bullets: List<String>,
    val chart: CoachChart?,
    val safetyNote: String?,
    val dataQualityNote: String?,
    val sources: List<CoachSource>,
    val followUpChips: List<String>,
    val actionsTaken: List<String>,
    val confidence: String
) {
    companion object {
        fun fromJson(json: String): CoachResponse {
            val o = JSONObject(json)

            // title: canonical field, fall back to "headline" if the model drifts
            val title = o.safeString("title")
                ?: o.safeString("headline")
                ?: ""

            // summary: canonical field; fall back to "message" (string form) if missing
            val msgRaw = o.opt("message")
            val messageString = (msgRaw as? String)?.takeIf { it.isNotBlank() && it != "null" } ?: ""
            val messageList   = if (msgRaw is org.json.JSONArray)
                (0 until msgRaw.length()).map { msgRaw.optString(it) }.filter { it.isNotBlank() && it != "null" }
            else emptyList()

            val summary = o.safeString("summary")
                ?: messageString.takeIf { it.isNotEmpty() }
                ?: messageList.joinToString("\n").takeIf { it.isNotEmpty() }
                ?: ""

            // bullets: canonical field; fall back to "insights", "recommendations", or message-as-list
            val bullets = stringList(o, "bullets").takeIf { it.isNotEmpty() }
                ?: stringList(o, "insights").takeIf { it.isNotEmpty() }
                ?: (stringList(o, "recommendations") + messageList).takeIf { it.isNotEmpty() }
                ?: emptyList()

            return CoachResponse(
                responseType     = o.safeString("response_type") ?: "summary",
                title            = title,
                summary          = summary,
                bullets          = bullets,
                chart            = if (o.isNull("chart")) null else o.optJSONObject("chart")?.let(CoachChart::fromJson),
                safetyNote       = o.safeString("safety_note"),
                dataQualityNote  = o.safeString("data_quality_note"),
                sources          = sourceList(o),
                followUpChips    = stringList(o, "follow_up_chips"),
                actionsTaken     = stringList(o, "actions_taken"),
                confidence       = o.safeString("confidence") ?: "medium"
            )
        }

        private fun stringList(o: JSONObject, key: String): List<String> {
            val arr = o.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                if (arr.isNull(i)) null
                else arr.optString(i).takeIf { it.isNotBlank() && it != "null" }
            }
        }

        private fun sourceList(o: JSONObject): List<CoachSource> {
            val arr = o.optJSONArray("sources") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { s ->
                    CoachSource(
                        tool    = s.safeString("tool") ?: return@mapNotNull null,
                        summary = s.safeString("summary") ?: ""
                    )
                }
            }
        }

        // org.json.JSONObject.optString() returns the literal string "null" when the JSON
        // value is null. This helper returns Kotlin null in that case.
        private fun JSONObject.safeString(key: String): String? {
            if (isNull(key)) return null
            return optString(key).takeIf { it.isNotBlank() && it != "null" }
        }
    }
}

data class CoachChart(
    val type: String,
    val title: String,
    val unit: String,
    val data: List<CoachChartPoint>
) {
    companion object {
        fun fromJson(o: JSONObject): CoachChart {
            val dataArr = o.optJSONArray("data")
            val points = if (dataArr != null) {
                (0 until dataArr.length()).mapNotNull { i ->
                    dataArr.optJSONObject(i)?.let { p ->
                        CoachChartPoint(label = p.optString("label"), value = p.optDouble("value", 0.0))
                    }
                }
            } else emptyList()
            return CoachChart(
                type  = o.optString("type", "bar"),
                title = o.optString("title", ""),
                unit  = o.optString("unit", ""),
                data  = points
            )
        }
    }
}

data class CoachChartPoint(val label: String, val value: Double)
data class CoachSource(val tool: String, val summary: String)
