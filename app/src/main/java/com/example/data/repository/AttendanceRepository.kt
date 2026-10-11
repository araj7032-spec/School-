package com.example.data.repository

import com.example.data.dao.AttendanceDao
import com.example.data.dao.SchoolSettingsDao
import com.example.data.dao.TeacherDao
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.SchoolSettingsEntity
import com.example.data.model.TeacherEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceRepository(
    private val teacherDao: TeacherDao,
    private val attendanceDao: AttendanceDao,
    private val schoolSettingsDao: SchoolSettingsDao
) {
    val allTeachers: Flow<List<TeacherEntity>> = teacherDao.getAllTeachers()
    val schoolSettings: Flow<SchoolSettingsEntity?> = schoolSettingsDao.getSettings()

    suspend fun getTeacherByPhone(phone: String): TeacherEntity? {
        return teacherDao.getTeacherByPhone(phone.trim())
    }

    suspend fun getTeacherById(id: Long): TeacherEntity? {
        return teacherDao.getTeacherById(id)
    }

    suspend fun registerTeacher(teacher: TeacherEntity): Long {
        return teacherDao.insertTeacher(teacher)
    }

    suspend fun updateTeacher(teacher: TeacherEntity) {
        teacherDao.updateTeacher(teacher)
    }

    suspend fun deleteTeacher(teacher: TeacherEntity) {
        teacherDao.deleteTeacher(teacher)
    }

    suspend fun getTodayRecord(teacherId: Long): AttendanceRecordEntity? {
        val todayStr = getTodayDateString()
        return attendanceDao.getRecordForDate(teacherId, todayStr)
    }

    fun getRecordsForTeacher(teacherId: Long): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getRecordsByTeacher(teacherId)
    }

    fun getTodayAllRecords(): Flow<List<AttendanceRecordEntity>> {
        val todayStr = getTodayDateString()
        return attendanceDao.getRecordsForDate(todayStr)
    }

    fun getRecordsForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getRecordsForMonth(monthPrefix)
    }

    fun getAllRecords(): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAllRecords()
    }

    suspend fun updateSettings(settings: SchoolSettingsEntity) {
        schoolSettingsDao.insertOrUpdate(settings)
    }

    suspend fun getSettingsSync(): SchoolSettingsEntity {
        return schoolSettingsDao.getSettingsSync() ?: SchoolSettingsEntity()
    }

    suspend fun punchIn(
        teacher: TeacherEntity,
        lat: Double,
        lng: Double,
        isGeofenceVerified: Boolean
    ): Result<AttendanceRecordEntity> {
        val todayStr = getTodayDateString()
        val existing = attendanceDao.getRecordForDate(teacher.id, todayStr)
        if (existing?.punchInTimestamp != null) {
            return Result.failure(IllegalStateException("Already punched in today at ${formatTime(existing.punchInTimestamp)}"))
        }

        val settings = getSettingsSync()
        val now = System.currentTimeMillis()

        // Calculate if late
        val (isLate, minutesLate) = evaluatePunchInShift(now, settings.shiftStartTime, settings.lateGraceMinutes)

        val record = AttendanceRecordEntity(
            id = existing?.id ?: 0,
            teacherId = teacher.id,
            teacherName = teacher.fullName,
            teacherPhone = teacher.phoneNumber,
            date = todayStr,
            punchInTimestamp = now,
            punchOutTimestamp = null,
            punchInLatitude = lat,
            punchInLongitude = lng,
            punchOutLatitude = null,
            punchOutLongitude = null,
            punchInStatus = if (isLate) "LATE" else "ON_TIME",
            punchOutStatus = "PENDING",
            minutesLate = minutesLate,
            minutesEarly = 0,
            totalMinutesWorked = 0,
            isGeofenceVerified = isGeofenceVerified
        )

        val newId = attendanceDao.insert(record)
        return Result.success(record.copy(id = if (record.id == 0L) newId else record.id))
    }

    suspend fun punchOut(
        teacher: TeacherEntity,
        lat: Double,
        lng: Double,
        isGeofenceVerified: Boolean
    ): Result<AttendanceRecordEntity> {
        val todayStr = getTodayDateString()
        val existing = attendanceDao.getRecordForDate(teacher.id, todayStr)
            ?: return Result.failure(IllegalStateException("Must punch in first before punching out."))

        if (existing.punchInTimestamp == null) {
            return Result.failure(IllegalStateException("No punch-in recorded for today."))
        }

        val settings = getSettingsSync()
        val now = System.currentTimeMillis()

        // Calculate if early departure
        val (isEarly, minutesEarly) = evaluatePunchOutShift(now, settings.shiftEndTime, settings.earlyGraceMinutes)

        val totalMinutes = ((now - existing.punchInTimestamp) / 60000).toInt().coerceAtLeast(1)

        val updated = existing.copy(
            punchOutTimestamp = now,
            punchOutLatitude = lat,
            punchOutLongitude = lng,
            punchOutStatus = if (isEarly) "EARLY_EXIT" else "ON_TIME",
            minutesEarly = minutesEarly,
            totalMinutesWorked = totalMinutes,
            isGeofenceVerified = existing.isGeofenceVerified && isGeofenceVerified
        )

        attendanceDao.update(updated)
        return Result.success(updated)
    }

    private fun evaluatePunchInShift(
        currentTimeMs: Long,
        shiftStartTime: String,
        lateGraceMinutes: Int
    ): Pair<Boolean, Int> {
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        val parts = shiftStartTime.split(":")
        val shiftHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val shiftMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30

        val shiftCal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            set(Calendar.HOUR_OF_DAY, shiftHour)
            set(Calendar.MINUTE, shiftMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val graceCutoffMs = shiftCal.timeInMillis + (lateGraceMinutes * 60 * 1000L)
        return if (currentTimeMs > graceCutoffMs) {
            val lateDiffMinutes = ((currentTimeMs - shiftCal.timeInMillis) / 60000).toInt()
            Pair(true, lateDiffMinutes)
        } else {
            Pair(false, 0)
        }
    }

    private fun evaluatePunchOutShift(
        currentTimeMs: Long,
        shiftEndTime: String,
        earlyGraceMinutes: Int
    ): Pair<Boolean, Int> {
        val parts = shiftEndTime.split(":")
        val shiftHour = parts.getOrNull(0)?.toIntOrNull() ?: 15
        val shiftMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30

        val shiftCal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            set(Calendar.HOUR_OF_DAY, shiftHour)
            set(Calendar.MINUTE, shiftMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val earlyCutoffMs = shiftCal.timeInMillis - (earlyGraceMinutes * 60 * 1000L)
        return if (currentTimeMs < earlyCutoffMs) {
            val earlyDiffMinutes = ((shiftCal.timeInMillis - currentTimeMs) / 60000).toInt()
            Pair(true, earlyDiffMinutes)
        } else {
            Pair(false, 0)
        }
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Calendar.getInstance().time)
        }

        fun getCurrentMonthPrefix(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            return sdf.format(Calendar.getInstance().time)
        }

        fun formatTime(ms: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(ms)
        }

        fun formatDatePretty(dateStr: String): String {
            return try {
                val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outSdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                val date = inSdf.parse(dateStr)
                if (date != null) outSdf.format(date) else dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}
