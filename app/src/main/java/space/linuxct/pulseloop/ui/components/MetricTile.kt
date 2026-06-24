package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

@Composable
fun MetricTile(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    trend: List<Double> = emptyList()
) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                if (unit != null) {
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                }
            }

            MiniSparkline(
                values = trend,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )
        }
    }
}
