package space.linuxct.pulseloop.ui.navigation

sealed class NavRoute(val route: String) {
    // Tabs (always in back stack)
    data object Today    : NavRoute("today")
    data object Vitals   : NavRoute("vitals")
    data object Activity : NavRoute("activity")
    data object Sleep    : NavRoute("sleep")
    data object Coach    : NavRoute("coach")

    // Pushed destinations
    data object Settings        : NavRoute("settings")
    data object DataExport      : NavRoute("data_export")
    data object Pairing         : NavRoute("pairing")
    data object Debug           : NavRoute("debug")
    data object RecordSelect    : NavRoute("record/select")

    data class ActivityDetail(val sessionId: String) : NavRoute("activity/detail/$sessionId") {
        companion object { const val PATTERN = "activity/detail/{sessionId}" }
    }
    data class RecordLive(val sessionId: String)    : NavRoute("record/live/$sessionId") {
        companion object { const val PATTERN = "record/live/{sessionId}" }
    }
    data class RecordSummary(val sessionId: String) : NavRoute("record/summary/$sessionId") {
        companion object { const val PATTERN = "record/summary/{sessionId}" }
    }
}
