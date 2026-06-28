package space.linuxct.pulseloop.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiMode { LEGACY, MATERIAL_YOU }

val LocalUiMode = staticCompositionLocalOf { UiMode.LEGACY }
