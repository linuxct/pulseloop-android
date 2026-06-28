package space.linuxct.pulseloop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import space.linuxct.pulseloop.ui.components.v1.PrimaryButtonV1
import space.linuxct.pulseloop.ui.components.v1.SecondaryButtonV1
import space.linuxct.pulseloop.ui.components.v2.PrimaryButtonV2
import space.linuxct.pulseloop.ui.components.v2.SecondaryButtonV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode

@Composable
fun PrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        PrimaryButtonV2(title, onClick, modifier, iconRes, enabled, compact)
    } else {
        PrimaryButtonV1(title, onClick, modifier, iconRes, enabled)
    }
}

@Composable
fun SecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        SecondaryButtonV2(title, onClick, modifier, iconRes, enabled, compact)
    } else {
        SecondaryButtonV1(title, onClick, modifier, iconRes, enabled)
    }
}
