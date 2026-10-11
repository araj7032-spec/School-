package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeofenceStatus
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError

@Composable
fun GeofenceRadarCard(
    geofenceStatus: GeofenceStatus?,
    simulationMode: Int,
    onSetSimulationMode: (Int) -> Unit,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTestingControls by remember { mutableStateOf(false) }

    val isInside = geofenceStatus?.isInside == true
    val distance = geofenceStatus?.distanceMeters?.toInt() ?: 0
    val radius = geofenceStatus?.allowedRadius?.toInt() ?: 200

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("geofence_radar_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Title & Refresh & Test toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isInside) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Geofence Status",
                            tint = if (isInside) EmeraldSuccess else RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Geofenced Boundary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isInside) "Inside School Perimeter" else "Outside Perimeter Restriction",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isInside) EmeraldSuccess else RoseError,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showTestingControls = !showTestingControls },
                        modifier = Modifier.testTag("toggle_geo_sim_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Simulate Location",
                            tint = if (simulationMode != 0) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshLocation,
                        modifier = Modifier.testTag("refresh_location_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh GPS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radar & Distance View
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Radar Canvas
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxRadius = size.width / 2

                        // Concentric range circles
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            radius = maxRadius * 0.4f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            radius = maxRadius * 0.75f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // School Geofence boundary circle
                        drawCircle(
                            color = (if (isInside) EmeraldSuccess else RoseError).copy(alpha = 0.2f),
                            radius = maxRadius * 0.7f,
                            center = center
                        )
                        drawCircle(
                            color = if (isInside) EmeraldSuccess else RoseError,
                            radius = maxRadius * 0.7f,
                            center = center,
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Center school marker
                        drawCircle(
                            color = IndigoPrimary,
                            radius = 4.dp.toPx(),
                            center = center
                        )

                        // User position marker
                        val userOffset = if (isInside) {
                            Offset(center.x + 8.dp.toPx(), center.y - 6.dp.toPx())
                        } else {
                            Offset(center.x + 32.dp.toPx(), center.y - 28.dp.toPx())
                        }
                        drawCircle(
                            color = if (isInside) EmeraldSuccess else RoseError,
                            radius = 5.dp.toPx(),
                            center = userOffset
                        )
                    }

                    // Center School Pin Icon
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "School Center",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stats breakdown
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Distance:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${distance}m",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isInside) EmeraldSuccess else RoseError
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Allowed Perimeter:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${radius}m radius",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isInside) EmeraldSuccess.copy(alpha = 0.14f) else RoseError.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isInside) "✓ Biometric Attendance Allowed" else "✕ Move closer to school to punch",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isInside) EmeraldSuccess else RoseError,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            // Testing / Simulation panel (toggleable)
            AnimatedVisibility(visible = showTestingControls) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Tester Geofence Simulator (Browser / Emulator):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ElevatedFilterChip(
                            selected = simulationMode == 0,
                            onClick = { onSetSimulationMode(0) },
                            label = { Text("Real GPS", fontSize = 11.sp) },
                            modifier = Modifier.testTag("geo_mode_gps")
                        )
                        ElevatedFilterChip(
                            selected = simulationMode == 1,
                            onClick = { onSetSimulationMode(1) },
                            label = { Text("Inside School", fontSize = 11.sp) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = EmeraldSuccess.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldSuccess
                            ),
                            modifier = Modifier.testTag("geo_mode_inside")
                        )
                        ElevatedFilterChip(
                            selected = simulationMode == 2,
                            onClick = { onSetSimulationMode(2) },
                            label = { Text("Outside (+1.8km)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = RoseError.copy(alpha = 0.2f),
                                selectedLabelColor = RoseError
                            ),
                            modifier = Modifier.testTag("geo_mode_outside")
                        )
                    }
                }
            }
        }
    }
}
