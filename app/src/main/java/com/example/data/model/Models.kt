package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val fullName: String,
    val teacherId: String,
    val department: String,
    val role: String = "TEACHER", // TEACHER or ADMIN
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val teacherName: String,
    val teacherPhone: String,
    val date: String, // YYYY-MM-DD
    val punchInTimestamp: Long?,
    val punchOutTimestamp: Long?,
    val punchInLatitude: Double?,
    val punchInLongitude: Double?,
    val punchOutLatitude: Double?,
    val punchOutLongitude: Double?,
    val punchInStatus: String, // ON_TIME, LATE, NOT_RECORDED
    val punchOutStatus: String, // ON_TIME, EARLY_EXIT, PENDING
    val minutesLate: Int = 0,
    val minutesEarly: Int = 0,
    val totalMinutesWorked: Int = 0,
    val isGeofenceVerified: Boolean = true
)

@Entity(tableName = "school_settings")
data class SchoolSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val schoolName: String = "St. Mary Higher Secondary School",
    val latitude: Double = 37.4220, // Default to a standard coordinate (e.g. Mountain View / customizable)
    val longitude: Double = -122.0841,
    val radiusMeters: Double = 200.0, // 200m radius
    val shiftStartTime: String = "08:30", // HH:mm (24-hr)
    val shiftEndTime: String = "15:30", // HH:mm
    val lateGraceMinutes: Int = 15, // Up to 08:45 is on time
    val earlyGraceMinutes: Int = 10,
    val adminPin: String = "1234",
    val simulationBypassEnabled: Boolean = false // Allows testing on emulator outside school
)

enum class PunchType {
    IN, OUT
}

data class GeofenceStatus(
    val isInside: Boolean,
    val distanceMeters: Float,
    val currentLat: Double,
    val currentLng: Double,
    val schoolLat: Double,
    val schoolLng: Double,
    val allowedRadius: Double,
    val hasPermission: Boolean,
    val isSimulation: Boolean = false
)

data class ShiftConfig(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val lateGraceMinutes: Int
)

data class MonthlyTeacherSummary(
    val teacherId: Long,
    val teacherName: String,
    val teacherPhone: String,
    val department: String,
    val totalDaysPresent: Int,
    val totalDaysLate: Int,
    val totalEarlyExits: Int,
    val totalMinutesWorked: Int
)
