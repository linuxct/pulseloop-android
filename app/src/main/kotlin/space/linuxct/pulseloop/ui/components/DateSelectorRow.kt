package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import java.util.Calendar
import java.util.TimeZone

private fun utcMidnightToLocalMidnight(utcMs: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
    return Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun localMidnightToUtcMidnight(localMs: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMs }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarIconButton(
    selectedDateMs: Long?,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedDateMs != null
    var showPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMs?.let { localMidnightToUtcMidnight(it) } ?: System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val todayUtcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                return utcTimeMillis <= todayUtcMidnight
            }
            override fun isSelectableYear(year: Int): Boolean =
                year <= Calendar.getInstance().get(Calendar.YEAR)
        }
    )

    LaunchedEffect(selectedDateMs) {
        datePickerState.selectedDateMillis = selectedDateMs?.let { localMidnightToUtcMidnight(it) } ?: System.currentTimeMillis()
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    val pickedUtcMs = datePickerState.selectedDateMillis ?: return@TextButton
                    val localMidnight = utcMidnightToLocalMidnight(pickedUtcMs)
                    val todayMidnight = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onDateSelected(if (localMidnight >= todayMidnight) null else localMidnight)
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        val scheme = MaterialTheme.colorScheme
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) scheme.secondaryContainer else Color.Transparent)
                .border(1.25.dp, if (isSelected) Color.Transparent else scheme.outline, CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showPicker = true }
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (isSelected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        val colors = LocalPulseColors.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.accent else colors.cardSoft)
                .border(1.25.dp, if (isSelected) Color.Transparent else colors.borderSubtle, CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showPicker = true }
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (isSelected) Color.White else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
