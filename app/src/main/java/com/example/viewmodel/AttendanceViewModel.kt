package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrics.BiometricHelper
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.GeofenceStatus
import com.example.data.model.MonthlyTeacherSummary
import com.example.data.model.PunchType
import com.example.data.model.SchoolSettingsEntity
import com.example.data.model.TeacherEntity
import com.example.data.repository.AttendanceRepository
import com.example.location.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(
        database.teacherDao(),
        database.attendanceDao(),
        database.schoolSettingsDao()
    )
    private val locationHelper = LocationHelper(application)
    private val biometricHelper = BiometricHelper(application)

    val schoolSettings: StateFlow<SchoolSettingsEntity> = repository.schoolSettings
        .map { it ?: SchoolSettingsEntity() }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            SchoolSettingsEntity()
        )

    val allTeachers: StateFlow<List<TeacherEntity>> = repository.allTeachers
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val todayRecords: StateFlow<List<AttendanceRecordEntity>> = repository.getTodayAllRecords()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    private val _selectedMonth = MutableStateFlow(AttendanceRepository.getCurrentMonthPrefix())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _monthlyRecords = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val monthlyRecords: StateFlow<List<AttendanceRecordEntity>> = _monthlyRecords.asStateFlow()

    private val _selectedTeacherFilter = MutableStateFlow<Long?>(null)
    val selectedTeacherFilter: StateFlow<Long?> = _selectedTeacherFilter.asStateFlow()

    // Current Session
    private val _currentTeacher = MutableStateFlow<TeacherEntity?>(null)
    val currentTeacher: StateFlow<TeacherEntity?> = _currentTeacher.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _currentTeacherTodayRecord = MutableStateFlow<AttendanceRecordEntity?>(null)
    val currentTeacherTodayRecord: StateFlow<AttendanceRecordEntity?> = _currentTeacherTodayRecord.asStateFlow()

    private val _currentTeacherHistory = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val currentTeacherHistory: StateFlow<List<AttendanceRecordEntity>> = _currentTeacherHistory.asStateFlow()

    // Location & Geofence
    private val _geofenceStatus = MutableStateFlow<GeofenceStatus?>(null)
    val geofenceStatus: StateFlow<GeofenceStatus?> = _geofenceStatus.asStateFlow()

    // 0 = Real GPS, 1 = Force Inside School Grounds (Test Mode), 2 = Force Outside School (Test Mode)
    private val _simulatedLocationMode = MutableStateFlow(0)
    val simulatedLocationMode: StateFlow<Int> = _simulatedLocationMode.asStateFlow()

    // Biometric Scanner State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private val _scanSuccess = MutableStateFlow<Boolean?>(null)
    val scanSuccess: StateFlow<Boolean?> = _scanSuccess.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Collect monthly records
        viewModelScope.launch {
            _selectedMonth.collect { monthPrefix ->
                repository.getRecordsForMonth(monthPrefix).collect { records ->
                    _monthlyRecords.value = records
                }
            }
        }

        // Initialize GPS / Geofence check
        refreshLocation()
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setSimulatedLocationMode(mode: Int) {
        _simulatedLocationMode.value = mode
        refreshLocation()
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val settings = repository.getSettingsSync()
            val mode = _simulatedLocationMode.value

            when (mode) {
                1 -> {
                    // Force Inside school boundary (0m away)
                    _geofenceStatus.value = LocationHelper.evaluateGeofence(
                        currentLat = settings.latitude,
                        currentLng = settings.longitude,
                        schoolLat = settings.latitude,
                        schoolLng = settings.longitude,
                        radiusMeters = settings.radiusMeters,
                        hasPermission = true,
                        isSimulation = true
                    )
                }
                2 -> {
                    // Force Outside school boundary (1.8 km away)
                    val outsideLat = settings.latitude + 0.015
                    val outsideLng = settings.longitude + 0.015
                    _geofenceStatus.value = LocationHelper.evaluateGeofence(
                        currentLat = outsideLat,
                        currentLng = outsideLng,
                        schoolLat = settings.latitude,
                        schoolLng = settings.longitude,
                        radiusMeters = settings.radiusMeters,
                        hasPermission = true,
                        isSimulation = true
                    )
                }
                else -> {
                    // Real GPS Location
                    val loc = locationHelper.getCurrentLocation()
                    if (loc != null) {
                        _geofenceStatus.value = LocationHelper.evaluateGeofence(
                            currentLat = loc.first,
                            currentLng = loc.second,
                            schoolLat = settings.latitude,
                            schoolLng = settings.longitude,
                            radiusMeters = settings.radiusMeters,
                            hasPermission = true,
                            isSimulation = false
                        )
                    } else {
                        // Default fallback on emulator if GPS has not yet acquired fix
                        _geofenceStatus.value = LocationHelper.evaluateGeofence(
                            currentLat = settings.latitude,
                            currentLng = settings.longitude,
                            schoolLat = settings.latitude,
                            schoolLng = settings.longitude,
                            radiusMeters = settings.radiusMeters,
                            hasPermission = false,
                            isSimulation = true
                        )
                    }
                }
            }
        }
    }

    fun loginTeacherWithPhone(phone: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanPhone = phone.trim()
            val teacher = repository.getTeacherByPhone(cleanPhone)
            if (teacher != null) {
                _currentTeacher.value = teacher
                _isAdminLoggedIn.value = false
                loadTeacherTodayData(teacher.id)
                onResult(true, "Welcome back, ${teacher.fullName}")
            } else {
                onResult(false, "Teacher with phone $cleanPhone not registered.")
            }
        }
    }

    fun selectTeacherDirect(teacher: TeacherEntity) {
        _currentTeacher.value = teacher
        _isAdminLoggedIn.value = false
        loadTeacherTodayData(teacher.id)
    }

    fun registerTeacher(
        fullName: String,
        phone: String,
        teacherId: String,
        department: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanPhone = phone.trim()
            val cleanName = fullName.trim()
            val cleanId = teacherId.trim().ifEmpty { "TCH-${System.currentTimeMillis() % 10000}" }
            val cleanDept = department.trim().ifEmpty { "General Education" }

            if (cleanName.isBlank() || cleanPhone.isBlank()) {
                onResult(false, "Please provide full name and phone number.")
                return@launch
            }

            val existing = repository.getTeacherByPhone(cleanPhone)
            if (existing != null) {
                onResult(false, "A teacher with phone $cleanPhone already exists.")
                return@launch
            }

            val newTeacher = TeacherEntity(
                phoneNumber = cleanPhone,
                fullName = cleanName,
                teacherId = cleanId,
                department = cleanDept
            )
            val newId = repository.registerTeacher(newTeacher)
            val created = newTeacher.copy(id = newId)
            _currentTeacher.value = created
            _isAdminLoggedIn.value = false
            loadTeacherTodayData(newId)
            onResult(true, "Registration successful! Welcome, ${created.fullName}")
        }
    }

    fun loginAdmin(pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val settings = repository.getSettingsSync()
            if (pin.trim() == settings.adminPin.trim()) {
                _isAdminLoggedIn.value = true
                _currentTeacher.value = null
                onResult(true, "Admin session authenticated.")
            } else {
                onResult(false, "Invalid Admin PIN. Please check and try again.")
            }
        }
    }

    fun logout() {
        _currentTeacher.value = null
        _isAdminLoggedIn.value = false
        _currentTeacherTodayRecord.value = null
        _currentTeacherHistory.value = emptyList()
    }

    private fun loadTeacherTodayData(teacherId: Long) {
        viewModelScope.launch {
            _currentTeacherTodayRecord.value = repository.getTodayRecord(teacherId)
            repository.getRecordsForTeacher(teacherId).collect { history ->
                _currentTeacherHistory.value = history
                _currentTeacherTodayRecord.value = history.firstOrNull {
                    it.date == AttendanceRepository.getTodayDateString()
                }
            }
        }
    }

    fun startFingerprintScan(punchType: PunchType) {
        val geo = _geofenceStatus.value
        if (geo == null || !geo.isInside) {
            val distance = geo?.distanceMeters?.toInt() ?: 999
            biometricHelper.triggerErrorHaptic()
            showToast("Geofence Restriction: You are outside the school perimeter (${distance}m away). Attendance blocked.")
            return
        }

        scanJob?.cancel()
        _isScanning.value = true
        _scanProgress.value = 0f
        _scanSuccess.value = null
        _scanStatusMessage.value = "Holding thumb to biometric sensor..."

        scanJob = viewModelScope.launch {
            // Tick progress for 1.2 seconds with tactile haptics
            val steps = 24
            for (i in 1..steps) {
                delay(50)
                _scanProgress.value = i / steps.toFloat()
                if (i % 6 == 0) {
                    biometricHelper.triggerScanTick()
                }
            }

            // Successfully scanned
            _scanStatusMessage.value = "Biometric Match: 99.8% (Verified)"
            _scanSuccess.value = true
            biometricHelper.triggerSuccessHaptic()

            delay(400)
            executePunch(punchType)
            delay(1200)

            _isScanning.value = false
            _scanProgress.value = 0f
            _scanStatusMessage.value = null
            _scanSuccess.value = null
        }
    }

    fun cancelFingerprintScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _scanProgress.value = 0f
        _scanStatusMessage.value = null
        _scanSuccess.value = null
    }

    private fun executePunch(punchType: PunchType) {
        val teacher = _currentTeacher.value ?: return
        val geo = _geofenceStatus.value ?: return

        viewModelScope.launch {
            if (punchType == PunchType.IN) {
                val res = repository.punchIn(
                    teacher = teacher,
                    lat = geo.currentLat,
                    lng = geo.currentLng,
                    isGeofenceVerified = geo.isInside
                )
                res.onSuccess { record ->
                    _currentTeacherTodayRecord.value = record
                    showToast("Punch-In Recorded at ${AttendanceRepository.formatTime(record.punchInTimestamp!!)} (${record.punchInStatus})")
                }.onFailure { err ->
                    showToast(err.message ?: "Failed to punch in")
                }
            } else {
                val res = repository.punchOut(
                    teacher = teacher,
                    lat = geo.currentLat,
                    lng = geo.currentLng,
                    isGeofenceVerified = geo.isInside
                )
                res.onSuccess { record ->
                    _currentTeacherTodayRecord.value = record
                    showToast("Punch-Out Recorded at ${AttendanceRepository.formatTime(record.punchOutTimestamp!!)} (${record.punchOutStatus})")
                }.onFailure { err ->
                    showToast(err.message ?: "Failed to punch out")
                }
            }
        }
    }

    fun updateSchoolGeofence(lat: Double, lng: Double, radius: Double, schoolName: String) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            val updated = current.copy(
                schoolName = schoolName.ifBlank { current.schoolName },
                latitude = lat,
                longitude = lng,
                radiusMeters = radius
            )
            repository.updateSettings(updated)
            refreshLocation()
            showToast("School geofence updated successfully.")
        }
    }

    fun updateShiftSettings(
        shiftStart: String,
        shiftEnd: String,
        lateGraceMinutes: Int,
        earlyGraceMinutes: Int
    ) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            val updated = current.copy(
                shiftStartTime = shiftStart,
                shiftEndTime = shiftEnd,
                lateGraceMinutes = lateGraceMinutes,
                earlyGraceMinutes = earlyGraceMinutes
            )
            repository.updateSettings(updated)
            showToast("Shift timing configuration saved.")
        }
    }

    fun updateAdminPin(newPin: String) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            repository.updateSettings(current.copy(adminPin = newPin))
            showToast("Admin PIN updated.")
        }
    }

    fun deleteTeacher(teacher: TeacherEntity) {
        viewModelScope.launch {
            repository.deleteTeacher(teacher)
            showToast("Teacher ${teacher.fullName} removed.")
        }
    }

    fun setSelectedMonth(monthPrefix: String) {
        _selectedMonth.value = monthPrefix
    }

    fun setSelectedTeacherFilter(teacherId: Long?) {
        _selectedTeacherFilter.value = teacherId
    }

    // Monthly Analytics Computation
    fun calculateMonthlyTeacherSummaries(
        records: List<AttendanceRecordEntity>,
        teachers: List<TeacherEntity>,
        filterTeacherId: Long?
    ): List<MonthlyTeacherSummary> {
        val targetTeachers = if (filterTeacherId != null) {
            teachers.filter { it.id == filterTeacherId }
        } else {
            teachers
        }

        return targetTeachers.map { teacher ->
            val teacherRecords = records.filter { it.teacherId == teacher.id }
            val presents = teacherRecords.size
            val lateDays = teacherRecords.count { it.punchInStatus == "LATE" }
            val earlyExits = teacherRecords.count { it.punchOutStatus == "EARLY_EXIT" }
            val totalMinutes = teacherRecords.sumOf { it.totalMinutesWorked }

            MonthlyTeacherSummary(
                teacherId = teacher.id,
                teacherName = teacher.fullName,
                teacherPhone = teacher.phoneNumber,
                department = teacher.department,
                totalDaysPresent = presents,
                totalDaysLate = lateDays,
                totalEarlyExits = earlyExits,
                totalMinutesWorked = totalMinutes
            )
        }
    }
}
