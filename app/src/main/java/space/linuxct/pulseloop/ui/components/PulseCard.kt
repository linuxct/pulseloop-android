package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    innerPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val colors = LocalPulseColors.current
    val bg = colors.card
    Box(
        modifier = modifier
            .clip(CardShape)
            .drawBehind { drawRect(bg) }
            .border(width = 1.dp, color = colors.borderSubtle, shape = CardShape)
            .padding(innerPadding)
    ) {
        content()
    }
}
