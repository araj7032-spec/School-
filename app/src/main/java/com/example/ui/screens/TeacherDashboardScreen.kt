package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.GeofenceStatus
import com.example.data.model.PunchType
import com.example.data.model.SchoolSettingsEntity
import com.example.data.model.TeacherEntity
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.FingerprintScannerButton
import com.example.ui.components.GeofenceRadarCard
import com.example.ui.components.ShiftBanner
import com.example.ui.components.StatusChip
import com.example.ui.components.TodayAttendanceCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    teacher: TeacherEntity,
    schoolSettings: SchoolSettingsEntity,
    todayRecord: AttendanceRecordEntity?,
    attendanceHistory: List<AttendanceRecordEntity>,
    geofenceStatus: GeofenceStatus?,
    simulationMode: Int,
    isScanning: Boolean,
    scanProgress: Float,
    scanSuccess: Boolean?,
    statusMessage: String?,
    onSetSimulationMode: (Int) -> Unit,
    onRefreshLocation: () -> Unit,
    onStartScan: (PunchType) -> Unit,
    onCancelScan: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPunchedIn = todayRecord?.punchInTimestamp != null
    val hasPunchedOut = todayRecord?.punchOutTimestamp != null
    val isCompleted = hasPunchedIn && hasPunchedOut

    val currentPunchType = if (!hasPunchedIn) PunchType.IN else PunchType.OUT

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "School",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = schoolSettings.schoolName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = "Teacher Attendance Portal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("teacher_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Switch Account / Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Teacher Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_profile_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = teacher.fullName,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teacher.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${teacher.department} • ID: ${teacher.teacherId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = teacher.phoneNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Verified Teacher",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Shift Banner
            item {
                ShiftBanner(
                    startTime = schoolSettings.shiftStartTime,
                    endTime = schoolSettings.shiftEndTime,
                    graceMinutes = schoolSettings.lateGraceMinutes
                )
            }

            // Geofence Radar Card
            item {
                GeofenceRadarCard(
                    geofenceStatus = geofenceStatus,
                    simulationMode = simulationMode,
                    onSetSimulationMode = onSetSimulationMode,
                    onRefreshLocation = onRefreshLocation
                )
            }

            // Today's Punch Activity
            item {
                TodayAttendanceCard(record = todayRecord)
            }

            // Fingerprint Biometric Scanner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FingerprintScannerButton(
                            punchType = currentPunchType,
                            isInsideGeofence = geofenceStatus?.isInside == true,
                            isScanning = isScanning,
                            scanProgress = scanProgress,
                            scanSuccess = scanSuccess,
                            statusMessage = statusMessage,
                            isCompletedForToday = isCompleted,
                            onStartScan = { onStartScan(currentPunchType) },
                            onCancelScan = onCancelScan
                        )
                    }
                }
            }

            // Attendance History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Attendance History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (attendanceHistory.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "No previous attendance records found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(attendanceHistory) { rec ->
                    HistoryItemCard(record = rec)
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: AttendanceRecordEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AttendanceRepository.formatDatePretty(record.date),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                val hours = record.totalMinutesWorked / 60
                val mins = record.totalMinutesWorked % 60
                Text(
                    text = if (record.totalMinutesWorked > 0) "${hours}h ${mins}m worked" else "Incomplete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Punch in time
                Column {
                    Text(
                        text = "Arrival: " + (record.punchInTimestamp?.let { AttendanceRepository.formatTime(it) } ?: "--"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Punch out time
                Column {
                    Text(
                        text = "Departure: " + (record.punchOutTimestamp?.let { AttendanceRepository.formatTime(it) } ?: "--"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Status chip
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (record.punchInStatus == "LATE") {
                        StatusChip(text = "Late (+${record.minutesLate}m)", isPositive = false)
                    } else {
                        StatusChip(text = "On-Time", isPositive = true)
                    }
                    if (record.punchOutStatus == "EARLY_EXIT") {
                        StatusChip(text = "Early Exit", isPositive = false)
                    }
                }
            }
        }
    }
}
