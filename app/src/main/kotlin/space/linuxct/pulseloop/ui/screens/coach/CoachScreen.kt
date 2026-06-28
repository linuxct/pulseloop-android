package space.linuxct.pulseloop.ui.screens.coach

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.ui.screens.coach.v1.CoachScreenV1
import space.linuxct.pulseloop.ui.screens.coach.v2.CoachScreenV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.CoachViewModel

@Composable
fun CoachScreen(navController: NavController, vm: CoachViewModel = hiltViewModel()) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        CoachScreenV2(navController, vm)
    } else {
        CoachScreenV1(navController, vm)
    }
}

/** Extracts a JSON string-field value from a potentially incomplete (mid-stream) JSON blob. */
internal fun extractJsonField(json: String, field: String): String {
    val key = "\"$field\":"
    val ki  = json.indexOf(key).takeIf { it >= 0 } ?: return ""
    var i   = ki + key.length
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != '"') return ""
    i++
    val sb = StringBuilder()
    while (i < json.length) {
        when {
            json[i] == '\\' && i + 1 < json.length -> {
                i++
                when (json[i]) {
                    '"'  -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n'  -> sb.append('\n')
                    't'  -> sb.append('\t')
                    'r'  -> sb.append('\r')
                    else -> { sb.append('\\'); sb.append(json[i]) }
                }
            }
            json[i] == '"' -> return sb.toString()
            else -> sb.append(json[i])
        }
        i++
    }
    return sb.toString()
}
