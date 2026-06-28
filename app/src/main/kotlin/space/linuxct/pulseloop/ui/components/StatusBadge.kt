package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class BadgeVariant { GOOD, WARN, NEUTRAL }

@Composable
fun StatusBadge(
    label: String,
    variant: BadgeVariant,
    modifier: Modifier = Modifier
) {
    val containerColor = when (variant) {
        BadgeVariant.GOOD    -> MaterialTheme.colorScheme.tertiaryContainer
        BadgeVariant.WARN    -> MaterialTheme.colorScheme.errorContainer
        BadgeVariant.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (variant) {
        BadgeVariant.GOOD    -> MaterialTheme.colorScheme.onTertiaryContainer
        BadgeVariant.WARN    -> MaterialTheme.colorScheme.onErrorContainer
        BadgeVariant.NEUTRAL -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
