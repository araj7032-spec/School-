package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.TeacherDashboardScreen
import com.example.ui.theme.BioAttendTheme
import com.example.viewmodel.AttendanceViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BioAttendTheme {
                BioAttendApp()
            }
        }
    }
}

@Composable
fun BioAttendApp(
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Request Fine & Coarse Location Permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // State Collection
    val currentUser by viewModel.currentTeacher.collectAsStateWithLifecycle()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
    val schoolSettings by viewModel.schoolSettings.collectAsStateWithLifecycle()
    val allTeachers by viewModel.allTeachers.collectAsStateWithLifecycle()
    val todayRecords by viewModel.todayRecords.collectAsStateWithLifecycle()
    val monthlyRecords by viewModel.monthlyRecords.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedTeacherFilter by viewModel.selectedTeacherFilter.collectAsStateWithLifecycle()
    val currentTeacherTodayRecord by viewModel.currentTeacherTodayRecord.collectAsStateWithLifecycle()
    val currentTeacherHistory by viewModel.currentTeacherHistory.collectAsStateWithLifecycle()
    val geofenceStatus by viewModel.geofenceStatus.collectAsStateWithLifecycle()
    val simulationMode by viewModel.simulatedLocationMode.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanSuccess by viewModel.scanSuccess.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val monthlySummaries = remember(monthlyRecords, allTeachers, selectedTeacherFilter) {
        viewModel.calculateMonthlyTeacherSummaries(monthlyRecords, allTeachers, selectedTeacherFilter)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            currentUser != null -> {
                BackHandler { viewModel.logout() }
                TeacherDashboardScreen(
                    teacher = currentUser!!,
                    schoolSettings = schoolSettings,
                    todayRecord = currentTeacherTodayRecord,
                    attendanceHistory = currentTeacherHistory,
                    geofenceStatus = geofenceStatus,
                    simulationMode = simulationMode,
                    isScanning = isScanning,
                    scanProgress = scanProgress,
                    scanSuccess = scanSuccess,
                    statusMessage = scanStatusMessage,
                    onSetSimulationMode = { viewModel.setSimulatedLocationMode(it) },
                    onRefreshLocation = { viewModel.refreshLocation() },
                    onStartScan = { punchType -> viewModel.startFingerprintScan(punchType) },
                    onCancelScan = { viewModel.cancelFingerprintScan() },
                    onLogout = { viewModel.logout() }
                )
            }
            isAdminLoggedIn -> {
                BackHandler { viewModel.logout() }
                AdminDashboardScreen(
                    schoolSettings = schoolSettings,
                    teachers = allTeachers,
                    todayRecords = todayRecords,
                    monthlyRecords = monthlyRecords,
                    selectedMonth = selectedMonth,
                    monthlySummaries = monthlySummaries,
                    selectedTeacherFilter = selectedTeacherFilter,
                    onSelectMonth = { viewModel.setSelectedMonth(it) },
                    onSelectTeacherFilter = { viewModel.setSelectedTeacherFilter(it) },
                    onUpdateGeofence = { lat, lng, radius, name ->
                        viewModel.updateSchoolGeofence(lat, lng, radius, name)
                    },
                    onUpdateShiftSettings = { start, end, lateGrace, earlyGrace ->
                        viewModel.updateShiftSettings(start, end, lateGrace, earlyGrace)
                    },
                    onUpdateAdminPin = { viewModel.updateAdminPin(it) },
                    onAddTeacher = { name, phone, id, dept ->
                        viewModel.registerTeacher(name, phone, id, dept) { ok, msg ->
                            viewModel.showToast(msg)
                        }
                    },
                    onDeleteTeacher = { viewModel.deleteTeacher(it) },
                    onUseCurrentGpsCoordinates = {
                        val st = geofenceStatus
                        if (st != null) Pair(st.currentLat, st.currentLng) else null
                    },
                    onLogout = { viewModel.logout() }
                )
            }
            else -> {
                LoginScreen(
                    sampleTeachers = allTeachers,
                    onLoginPhone = { phone ->
                        viewModel.loginTeacherWithPhone(phone) { ok, msg ->
                            if (!ok) viewModel.showToast(msg)
                        }
                    },
                    onRegister = { name, phone, id, dept ->
                        viewModel.registerTeacher(name, phone, id, dept) { ok, msg ->
                            viewModel.showToast(msg)
                        }
                    },
                    onAdminLogin = { pin ->
                        viewModel.loginAdmin(pin) { ok, msg ->
                            if (!ok) viewModel.showToast(msg)
                        }
                    },
                    onSelectSampleTeacher = { teacher ->
                        viewModel.selectTeacherDirect(teacher)
                    }
                )
            }
        }
    }
}
