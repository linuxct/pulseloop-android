package space.linuxct.pulseloop.coach.tools

import org.json.JSONObject

data class ToolResult(val json: String, val isError: Boolean = false) {
    companion object {
        fun success(obj: JSONObject) = ToolResult(obj.toString(), false)
        fun error(message: String) = ToolResult(JSONObject().put("error", message).toString(), true)
        fun raw(json: String) = ToolResult(json, false)
    }
}
