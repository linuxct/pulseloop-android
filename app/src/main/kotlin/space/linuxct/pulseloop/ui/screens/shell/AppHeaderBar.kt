package space.linuxct.pulseloop.ui.screens.shell

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.R
import androidx.hilt.navigation.compose.hiltViewModel
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.HeaderViewModel
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppHeaderBar(
    onSettingsTap: () -> Unit,
    onDebugTap: () -> Unit,
    vm: HeaderViewModel = hiltViewModel()
) {
    val colors = LocalPulseColors.current
    val uiMode  = LocalUiMode.current
    val state   by vm.connectionState.collectAsState()
    val battery by vm.batteryPercent.collectAsState()

    val headerContent: @Composable () -> Unit = {
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onDebugTap
            )
        ) {
            Text(
                text = stringResource(R.string.app_name_header),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                color = colors.textMuted
            )
            Text(
                text = stringResource(greetingForHour()),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ConnectionStatusPill(
                state = state,
                batteryPercent = battery,
                onClick = onSettingsTap
            )
            IconButton(onClick = onSettingsTap) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = colors.textSecondary
                )
            }
        }
    }
    }

    if (uiMode == UiMode.MATERIAL_YOU) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            headerContent()
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth().background(colors.background)) {
            headerContent()
        }
    }
}

@Composable
fun ConnectionStatusPill(
    state: RingConnectionState,
    batteryPercent: Int?,
    onClick: () -> Unit = {}
) {
    val colors = LocalPulseColors.current
    val isPulsing = state == RingConnectionState.CONNECTING ||
                    state == RingConnectionState.RECONNECTING ||
                    state == RingConnectionState.SCANNING

    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isPulsing) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot_alpha"
    )

    val dotColor = when (state) {
        RingConnectionState.CONNECTED    -> colors.success
        RingConnectionState.CONNECTING,
        RingConnectionState.RECONNECTING -> colors.accent
        RingConnectionState.SCANNING     -> colors.textMuted
        RingConnectionState.FAILED       -> colors.danger
        else                             -> colors.textMuted
    }

    val label = when (state) {
        RingConnectionState.CONNECTED    ->
            if (batteryPercent != null && batteryPercent > 0) stringResource(R.string.status_connected_battery, batteryPercent)
            else stringResource(R.string.status_connected)
        RingConnectionState.CONNECTING,
        RingConnectionState.RECONNECTING -> stringResource(R.string.status_connecting)
        RingConnectionState.SCANNING     -> stringResource(R.string.status_searching)
        RingConnectionState.FAILED       -> stringResource(R.string.status_sync_failed)
        else                             -> stringResource(R.string.status_disconnected)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.card)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor.copy(alpha = alpha), CircleShape)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            maxLines = 1
        )
    }
}

@StringRes
private fun greetingForHour(): Int {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        in 17..21 -> R.string.greeting_evening
        else      -> R.string.greeting_night
    }
}
