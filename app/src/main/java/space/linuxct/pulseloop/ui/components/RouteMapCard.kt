package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity

@Composable
fun RouteMapCard(
    points: List<ActivityGpsPointEntity>,
    height: Dp = 220.dp,
    interactive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val latLngs = remember(points) {
        points.filter { it.accepted }.sortedBy { it.timestamp }
            .map { LatLng(it.latitude, it.longitude) }
    }
    RouteMapCardLatLng(latLngs = latLngs, height = height, interactive = interactive, modifier = modifier)
}

@Composable
fun RouteMapCardLatLng(
    latLngs: List<LatLng>,
    height: Dp = 220.dp,
    interactive: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (latLngs.isEmpty()) return
    val context = LocalContext.current
    val darkStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLngs.first(), 15f)
    }

    LaunchedEffect(latLngs.size) {
        if (latLngs.size == 1) {
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 16f))
        } else {
            val bounds = latLngs.fold(LatLngBounds.builder()) { b, p -> b.include(p) }.build()
            runCatching { cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 72)) }
        }
    }

    PulseCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(height),
                cameraPositionState = cameraState,
                properties = MapProperties(
                    mapStyleOptions = darkStyle,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = interactive,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    zoomGesturesEnabled = interactive,
                    scrollGesturesEnabledDuringRotateOrZoom = false,
                    mapToolbarEnabled = false,
                    compassEnabled = false
                )
            ) {
                Polyline(
                    points = latLngs,
                    color = androidx.compose.ui.graphics.Color(0xFF7C5CFF),
                    width = 14f
                )
            }
        }
    }
}
