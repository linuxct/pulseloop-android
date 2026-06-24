package space.linuxct.pulseloop.coach.tools

object ToolRegistry {
    fun build(ctx: ToolContext): List<CoachTool> =
        RetrievalTools.build() + AnalysisTools.build() + MemoryTools.build()
}
