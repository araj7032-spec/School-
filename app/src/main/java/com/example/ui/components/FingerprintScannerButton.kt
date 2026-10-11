package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PunchType
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError

@Composable
fun FingerprintScannerButton(
    punchType: PunchType,
    isInsideGeofence: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    scanSuccess: Boolean?,
    statusMessage: String?,
    isCompletedForToday: Boolean,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            scanSuccess == true -> EmeraldSuccess
            isCompletedForToday -> MaterialTheme.colorScheme.surfaceVariant
            !isInsideGeofence -> RoseError.copy(alpha = 0.85f)
            isScanning -> IndigoPrimary
            else -> IndigoPrimary
        },
        label = "btnColor"
    )

    Column(
        modifier = modifier.testTag("fingerprint_scanner_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            // Animated ripple rings when scanning or ready
            if (isInsideGeofence && !isCompletedForToday) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2 * (if (isScanning) pulseScale else 1.05f)
                    drawCircle(
                        color = buttonColor.copy(alpha = if (isScanning) pulseAlpha else 0.15f),
                        radius = radius,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
            }

            // Circular progress indicator during scan
            if (isScanning) {
                CircularProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier.size(170.dp),
                    color = EmeraldSuccess,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = buttonColor.copy(alpha = 0.2f)
                )
            }

            // Core Scanner Circle
            Surface(
                shape = CircleShape,
                color = buttonColor.copy(alpha = if (isCompletedForToday) 0.3f else 0.18f),
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = buttonColor,
                        shape = CircleShape
                    )
                    .testTag("biometric_touch_pad")
                    .pointerInput(isInsideGeofence, isCompletedForToday, isScanning) {
                        if (isInsideGeofence && !isCompletedForToday) {
                            detectTapGestures(
                                onPress = {
                                    onStartScan()
                                    tryAwaitRelease()
                                    onCancelScan()
                                }
                            )
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        scanSuccess == true -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Biometric Verified",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                        isCompletedForToday -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Day Complete",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                        !isInsideGeofence -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Geofence Locked",
                                tint = RoseError,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Thumb Scan Sensor",
                                tint = buttonColor,
                                modifier = Modifier.size(76.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Prompt & Status Text
        val actionTitle = when {
            isCompletedForToday -> "Attendance Complete for Today"
            scanSuccess == true -> "Biometric Verification Success!"
            isScanning -> "Scanning Biometrics... ${(scanProgress * 100).toInt()}%"
            !isInsideGeofence -> "Scanner Locked: Outside School Grounds"
            punchType == PunchType.IN -> "Touch & Hold to Punch In"
            else -> "Touch & Hold to Punch Out"
        }

        Text(
            text = actionTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = when {
                scanSuccess == true -> EmeraldSuccess
                !isInsideGeofence -> RoseError
                isCompletedForToday -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.primary
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        val hint = when {
            isCompletedForToday -> "Arrival & Departure times are officially recorded"
            !isInsideGeofence -> "Move inside the designated school boundary to unlock"
            isScanning -> statusMessage ?: "Keep thumb pressed on the sensor"
            punchType == PunchType.IN -> "Press and hold thumb firmly on sensor"
            else -> "Mark end-of-day departure timestamp"
        }

        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
