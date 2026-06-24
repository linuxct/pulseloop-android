package space.linuxct.pulseloop.coach.tools

data class CoachTool(
    val name: String,
    val specJson: String,
    val run: suspend (arguments: String, ctx: ToolContext) -> ToolResult
)
