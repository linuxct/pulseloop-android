package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.ui.components.v1.PulseCardV1
import space.linuxct.pulseloop.ui.components.v2.PulseCardV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        PulseCardV2(modifier, innerPadding, onClick, content)
    } else {
        PulseCardV1(modifier, innerPadding, onClick, content)
    }
}
