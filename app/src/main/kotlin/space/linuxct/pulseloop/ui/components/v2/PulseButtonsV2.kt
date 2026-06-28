package space.linuxct.pulseloop.ui.components.v2

import androidx.compose.foundation.layout.PaddingValues
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

@Composable
internal fun PrimaryButtonV2(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(),
        contentPadding = if (compact) PaddingValues(horizontal = 20.dp, vertical = 0.dp) else ButtonDefaults.ContentPadding,
        modifier = if (compact) modifier.height(38.dp) else modifier.fillMaxWidth().height(56.dp)
    ) {
        if (iconRes != null) Icon(painter = painterResource(iconRes), contentDescription = null)
        Text(text = title, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Unspecified)
    }
}

@Composable
internal fun SecondaryButtonV2(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(),
        border = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding = if (compact) PaddingValues(horizontal = 20.dp, vertical = 0.dp) else ButtonDefaults.ContentPadding,
        modifier = if (compact) modifier.height(36.dp) else modifier.fillMaxWidth().height(52.dp)
    ) {
        if (iconRes != null) Icon(painter = painterResource(iconRes), contentDescription = null)
        Text(text = title, fontSize = if (compact) 14.sp else 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
