package space.linuxct.pulseloop.ui.components.v1

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.ui.components.CardShape
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

@Composable
internal fun PulseCardV1(
    modifier: Modifier = Modifier,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalPulseColors.current
    val bg = colors.card
    Box(
        modifier = modifier
            .clip(CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind { drawRect(bg) }
            .border(width = 1.dp, color = colors.borderSubtle, shape = CardShape)
            .padding(innerPadding)
    ) {
        content()
    }
}
