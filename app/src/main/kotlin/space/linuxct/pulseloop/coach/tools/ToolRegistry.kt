package space.linuxct.pulseloop.coach.tools

object ToolRegistry {
    // The shared metric enum baked into the tool specs, with and without the estimated BP/blood-sugar
    // metrics. Must match the enum strings in AnalysisTools/RetrievalTools verbatim.
    private const val ENUM_WITH_BLOOD =
        """["hr","spo2","hrv","stress","temp","bp_sys","bp_dia","glucose","fatigue","steps","calories","distance","active_minutes"]"""
    private const val ENUM_NO_BLOOD =
        """["hr","spo2","hrv","stress","temp","fatigue","steps","calories","distance","active_minutes"]"""

    fun build(ctx: ToolContext): List<CoachTool> {
        val tools = RetrievalTools.build() + AnalysisTools.build() + MemoryTools.build()
        // When the estimated BP/blood-sugar feature is off, strip those metrics from every tool's
        // schema so the model cannot request them and is not even aware they exist.
        return if (ctx.bloodMetricsEnabled) tools
        else tools.map { it.copy(specJson = it.specJson.replace(ENUM_WITH_BLOOD, ENUM_NO_BLOOD)) }
    }
}
