package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.MonthlyTeacherSummary
import com.example.data.model.SchoolSettingsEntity
import com.example.data.model.TeacherEntity
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.AmberLate
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    schoolSettings: SchoolSettingsEntity,
    teachers: List<TeacherEntity>,
    todayRecords: List<AttendanceRecordEntity>,
    monthlyRecords: List<AttendanceRecordEntity>,
    selectedMonth: String,
    monthlySummaries: List<MonthlyTeacherSummary>,
    selectedTeacherFilter: Long?,
    onSelectMonth: (String) -> Unit,
    onSelectTeacherFilter: (Long?) -> Unit,
    onUpdateGeofence: (lat: Double, lng: Double, radius: Double, schoolName: String) -> Unit,
    onUpdateShiftSettings: (start: String, end: String, lateGrace: Int, earlyGrace: Int) -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onAddTeacher: (name: String, phone: String, id: String, dept: String) -> Unit,
    onDeleteTeacher: (TeacherEntity) -> Unit,
    onUseCurrentGpsCoordinates: () -> Pair<Double, Double>?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Today Live, 1: Monthly Analytics, 2: Geofence & Shifts, 3: Teachers Directory
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Control Dashboard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                fontSize = 16.sp
                            )
                            Text(
                                text = schoolSettings.schoolName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Exit Admin",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Today") },
                    label = { Text("Today Live") },
                    modifier = Modifier.testTag("nav_today_live")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                    label = { Text("Geofence/Shift") },
                    modifier = Modifier.testTag("nav_settings")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Groups, contentDescription = "Teachers") },
                    label = { Text("Teachers") },
                    modifier = Modifier.testTag("nav_teachers")
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 3) {
                FloatingActionButton(
                    onClick = { showAddTeacherDialog = true },
                    containerColor = IndigoPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_teacher")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Teacher")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> AdminTodayLiveTab(
                    teachers = teachers,
                    todayRecords = todayRecords,
                    schoolSettings = schoolSettings
                )
                1 -> AdminMonthlyReportsTab(
                    monthlyRecords = monthlyRecords,
                    teachers = teachers,
                    monthlySummaries = monthlySummaries,
                    selectedMonth = selectedMonth,
                    selectedTeacherFilter = selectedTeacherFilter,
                    onSelectMonth = onSelectMonth,
                    onSelectTeacherFilter = onSelectTeacherFilter,
                    onOpenExport = { showExportDialog = true }
                )
                2 -> AdminGeofenceAndShiftTab(
                    schoolSettings = schoolSettings,
                    onUpdateGeofence = onUpdateGeofence,
                    onUpdateShiftSettings = onUpdateShiftSettings,
                    onUpdateAdminPin = onUpdateAdminPin,
                    onUseCurrentGpsCoordinates = onUseCurrentGpsCoordinates
                )
                3 -> AdminTeachersDirectoryTab(
                    teachers = teachers,
                    onDeleteTeacher = onDeleteTeacher
                )
            }
        }
    }

    // Add Teacher Dialog
    if (showAddTeacherDialog) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newId by remember { mutableStateOf("") }
        var newDept by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTeacherDialog = false },
            title = { Text("Register New Teacher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_teacher_name")
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_teacher_phone")
                    )
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        label = { Text("Teacher ID") },
                        placeholder = { Text("e.g. TCH-109") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_teacher_id")
                    )
                    OutlinedTextField(
                        value = newDept,
                        onValueChange = { newDept = it },
                        label = { Text("Department / Subject") },
                        placeholder = { Text("e.g. Mathematics") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_teacher_dept")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            onAddTeacher(newName, newPhone, newId, newDept)
                            showAddTeacherDialog = false
                        }
                    },
                    enabled = newName.isNotBlank() && newPhone.isNotBlank(),
                    modifier = Modifier.testTag("dialog_confirm_add_teacher")
                ) {
                    Text("Add Teacher")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTeacherDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Monthly Summary Dialog
    if (showExportDialog) {
        val totalPresent = monthlySummaries.sumOf { it.totalDaysPresent }
        val totalLate = monthlySummaries.sumOf { it.totalDaysLate }
        val totalEarly = monthlySummaries.sumOf { it.totalEarlyExits }
        val totalHours = monthlySummaries.sumOf { it.totalMinutesWorked } / 60

        val reportSummaryText = buildString {
            appendLine("=== SCHOOL ATTENDANCE MONTHLY REPORT ===")
            appendLine("School: ${schoolSettings.schoolName}")
            appendLine("Month: $selectedMonth")
            appendLine("Total Faculty Members: ${teachers.size}")
            appendLine("Total Recorded Attendances: $totalPresent")
            appendLine("Late Arrivals Flagged: $totalLate")
            appendLine("Early Departures: $totalEarly")
            appendLine("Total Faculty Hours Logged: ${totalHours}h")
            appendLine("----------------------------------------")
            appendLine("TEACHER BREAKDOWN:")
            monthlySummaries.forEach { s ->
                val h = s.totalMinutesWorked / 60
                val m = s.totalMinutesWorked % 60
                appendLine("- ${s.teacherName} (${s.department}): ${s.totalDaysPresent} Presents | ${s.totalDaysLate} Late | ${h}h ${m}m")
            }
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Monthly Report Summary") },
            text = {
                Column {
                    Text(
                        text = "Prepared report for $selectedMonth ready for download or clipboard copy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = reportSummaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Attendance Report", reportSummaryText)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = false
                    },
                    modifier = Modifier.testTag("copy_report_btn")
                ) {
                    Text("Copy Report Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ======================== TAB 0: TODAY LIVE ========================
@Composable
fun AdminTodayLiveTab(
    teachers: List<TeacherEntity>,
    todayRecords: List<AttendanceRecordEntity>,
    schoolSettings: SchoolSettingsEntity
) {
    val presentCount = todayRecords.size
    val lateCount = todayRecords.count { it.punchInStatus == "LATE" }
    val earlyExitCount = todayRecords.count { it.punchOutStatus == "EARLY_EXIT" }
    val absentCount = (teachers.size - presentCount).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Today's Live Attendance",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${AttendanceRepository.formatDatePretty(AttendanceRepository.getTodayDateString())} • Shift starts at ${schoolSettings.shiftStartTime} AM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4 KPI Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Present",
                    value = "$presentCount",
                    subtitle = "Checked In",
                    icon = Icons.Default.CheckCircle,
                    accentColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Late",
                    value = "$lateCount",
                    subtitle = "Past Grace",
                    icon = Icons.Default.Warning,
                    accentColor = AmberLate,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Early Exit",
                    value = "$earlyExitCount",
                    subtitle = "Before End",
                    icon = Icons.Default.Timer,
                    accentColor = RoseError,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Pending",
                    value = "$absentCount",
                    subtitle = "Not Punched",
                    icon = Icons.Default.Person,
                    accentColor = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Faculty Punch Status (${presentCount}/${teachers.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (teachers.isEmpty()) {
            item {
                Text(
                    text = "No teachers registered in the database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(teachers) { teacher ->
                val record = todayRecords.firstOrNull { it.teacherId == teacher.id }
                AdminTeacherTodayRow(teacher = teacher, record = record)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun AdminTeacherTodayRow(
    teacher: TeacherEntity,
    record: AttendanceRecordEntity?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (record != null) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = teacher.fullName,
                    tint = if (record != null) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.fullName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${teacher.department} • ${teacher.phoneNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (record?.punchInTimestamp != null) {
                    val inStr = AttendanceRepository.formatTime(record.punchInTimestamp)
                    val outStr = record.punchOutTimestamp?.let { AttendanceRepository.formatTime(it) } ?: "Working..."
                    Text(
                        text = "In: $inStr  |  Out: $outStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "No punch recorded today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Badges
            Column(horizontalAlignment = Alignment.End) {
                if (record != null) {
                    if (record.punchInStatus == "LATE") {
                        StatusChip(text = "Late (+${record.minutesLate}m)", isPositive = false)
                    } else {
                        StatusChip(text = "On-Time", isPositive = true)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (record.isGeofenceVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndigoPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "GPS Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Absent / Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ======================== TAB 1: MONTHLY REPORTS ========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonthlyReportsTab(
    monthlyRecords: List<AttendanceRecordEntity>,
    teachers: List<TeacherEntity>,
    monthlySummaries: List<MonthlyTeacherSummary>,
    selectedMonth: String,
    selectedTeacherFilter: Long?,
    onSelectMonth: (String) -> Unit,
    onSelectTeacherFilter: (Long?) -> Unit,
    onOpenExport: () -> Unit
) {
    var teacherDropdownExpanded by remember { mutableStateOf(false) }

    val totalDaysPresent = monthlySummaries.sumOf { it.totalDaysPresent }
    val totalLateDays = monthlySummaries.sumOf { it.totalDaysLate }
    val totalEarlyExits = monthlySummaries.sumOf { it.totalEarlyExits }
    val totalHoursLogged = monthlySummaries.sumOf { it.totalMinutesWorked } / 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Reports & Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Track faculty attendance, hours, and punctuality",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenExport,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("export_report_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month Pills & Teacher Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Month selector buttons
                val months = listOf("2026-10", "2026-09", "2026-08")
                months.forEach { m ->
                    val isSel = m == selectedMonth
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectMonth(m) }
                            .testTag("filter_month_$m")
                    ) {
                        Text(
                            text = m,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Monthly Overview KPI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Summary Metrics for $selectedMonth",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Attendances", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalDaysPresent", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                        }
                        Column {
                            Text("Late Flagged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalLateDays", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = AmberLate)
                        }
                        Column {
                            Text("Early Exits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalEarlyExits", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = RoseError)
                        }
                        Column {
                            Text("Total Hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalHoursLogged}h", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = IndigoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Punctuality Progress Bar
                    val onTimeRatio = if (totalDaysPresent > 0) {
                        (totalDaysPresent - totalLateDays).toFloat() / totalDaysPresent.toFloat()
                    } else 1f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Punctuality Rating:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(onTimeRatio * 100).toInt()}% On-Time",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (onTimeRatio >= 0.8f) EmeraldSuccess else AmberLate
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { onTimeRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Teacher-by-Teacher Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(monthlySummaries) { summary ->
            MonthlySummaryCard(summary = summary)
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun MonthlySummaryCard(summary: MonthlyTeacherSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = summary.teacherName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${summary.department} • ${summary.teacherPhone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val hours = summary.totalMinutesWorked / 60
                val mins = summary.totalMinutesWorked % 60
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IndigoPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${hours}h ${mins}m",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = IndigoPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: Presents, Late, Early Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Presents", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.totalDaysPresent}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Late Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.totalDaysLate}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AmberLate)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Early Exits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.totalEarlyExits}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = RoseError)
                    }
                }
            }
        }
    }
}

// ======================== TAB 2: GEOFENCE & SHIFT SETTINGS ========================
@Composable
fun AdminGeofenceAndShiftTab(
    schoolSettings: SchoolSettingsEntity,
    onUpdateGeofence: (lat: Double, lng: Double, radius: Double, schoolName: String) -> Unit,
    onUpdateShiftSettings: (start: String, end: String, lateGrace: Int, earlyGrace: Int) -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onUseCurrentGpsCoordinates: () -> Pair<Double, Double>?
) {
    var schoolNameInput by remember(schoolSettings) { mutableStateOf(schoolSettings.schoolName) }
    var latInput by remember(schoolSettings) { mutableStateOf(schoolSettings.latitude.toString()) }
    var lngInput by remember(schoolSettings) { mutableStateOf(schoolSettings.longitude.toString()) }
    var radiusSlider by remember(schoolSettings) { mutableFloatStateOf(schoolSettings.radiusMeters.toFloat()) }

    var shiftStartInput by remember(schoolSettings) { mutableStateOf(schoolSettings.shiftStartTime) }
    var shiftEndInput by remember(schoolSettings) { mutableStateOf(schoolSettings.shiftEndTime) }
    var lateGraceInput by remember(schoolSettings) { mutableStateOf(schoolSettings.lateGraceMinutes.toString()) }
    var earlyGraceInput by remember(schoolSettings) { mutableStateOf(schoolSettings.earlyGraceMinutes.toString()) }
    var adminPinInput by remember(schoolSettings) { mutableStateOf(schoolSettings.adminPin) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Geofence & Shift Management",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Admin rules controlling location perimeter and punctuality thresholds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // School Geofence Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Geofence",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "School Geofence Coordinates",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = schoolNameInput,
                        onValueChange = { schoolNameInput = it },
                        label = { Text("School / Campus Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setting_school_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("setting_lat_input")
                        )
                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { lngInput = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("setting_lng_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val cur = onUseCurrentGpsCoordinates()
                            if (cur != null) {
                                latInput = cur.first.toString()
                                lngInput = cur.second.toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("use_current_gps_btn")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use My GPS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Device's Current GPS Location")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Radius Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Geofence Radius:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${radiusSlider.toInt()} meters",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                    }

                    Slider(
                        value = radiusSlider,
                        onValueChange = { radiusSlider = it },
                        valueRange = 50f..1000f,
                        steps = 19,
                        modifier = Modifier.testTag("radius_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val lat = latInput.toDoubleOrNull() ?: schoolSettings.latitude
                            val lng = lngInput.toDoubleOrNull() ?: schoolSettings.longitude
                            onUpdateGeofence(lat, lng, radiusSlider.toDouble(), schoolNameInput)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_geofence_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Geofence Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shift Timings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Shift",
                            tint = CyanSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shift & Late-Comer Rules",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Set standard reporting and departure times. System flags late arrivals and early departures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = shiftStartInput,
                            onValueChange = { shiftStartInput = it },
                            label = { Text("Shift Start (HH:mm)") },
                            placeholder = { Text("08:30") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("setting_shift_start")
                        )
                        OutlinedTextField(
                            value = shiftEndInput,
                            onValueChange = { shiftEndInput = it },
                            label = { Text("Shift End (HH:mm)") },
                            placeholder = { Text("15:30") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("setting_shift_end")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = lateGraceInput,
                            onValueChange = { lateGraceInput = it },
                            label = { Text("Late Grace (mins)") },
                            placeholder = { Text("15") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("setting_late_grace")
                        )
                        OutlinedTextField(
                            value = earlyGraceInput,
                            onValueChange = { earlyGraceInput = it },
                            label = { Text("Early Grace (mins)") },
                            placeholder = { Text("10") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("setting_early_grace")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val lg = lateGraceInput.toIntOrNull() ?: 15
                            val eg = earlyGraceInput.toIntOrNull() ?: 10
                            onUpdateShiftSettings(shiftStartInput, shiftEndInput, lg, eg)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_shifts_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Shift Timings", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Security PIN Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Admin Security PIN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = { adminPinInput = it },
                        label = { Text("New PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("setting_admin_pin")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (adminPinInput.isNotBlank()) onUpdateAdminPin(adminPinInput)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_pin_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Update Security PIN")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ======================== TAB 3: TEACHERS DIRECTORY ========================
@Composable
fun AdminTeachersDirectoryTab(
    teachers: List<TeacherEntity>,
    onDeleteTeacher: (TeacherEntity) -> Unit
) {
    var teacherToDelete by remember { mutableStateOf<TeacherEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Faculty Directory (${teachers.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Manage registered teachers, phone credentials, and departments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (teachers.isEmpty()) {
            item {
                Text(
                    text = "No teachers currently registered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(teachers) { teacher ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = teacher.fullName,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teacher.fullName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${teacher.department} • ID: ${teacher.teacherId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = teacher.phoneNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { teacherToDelete = teacher },
                            modifier = Modifier.testTag("delete_teacher_${teacher.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Teacher",
                                tint = RoseError.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Leave space for FAB
        }
    }

    if (teacherToDelete != null) {
        val t = teacherToDelete!!
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text("Remove Teacher") },
            text = { Text("Are you sure you want to remove ${t.fullName} from the attendance database?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTeacher(t)
                        teacherToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = RoseError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
