package space.linuxct.pulseloop.ui.components.v1

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

@Composable
internal fun PrimaryButtonV1(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true
) {
    val colors = LocalPulseColors.current
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        if (iconRes != null) Icon(painter = painterResource(iconRes), contentDescription = null)
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
internal fun SecondaryButtonV1(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true
) {
    val colors = LocalPulseColors.current
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.textPrimary,
            containerColor = colors.card
        ),
        border = null,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, colors.borderSubtle, CircleShape)
    ) {
        if (iconRes != null) Icon(painter = painterResource(iconRes), contentDescription = null, tint = colors.textPrimary)
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}
