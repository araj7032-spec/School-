package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AttendanceDao
import com.example.data.dao.SchoolSettingsDao
import com.example.data.dao.TeacherDao
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.SchoolSettingsEntity
import com.example.data.model.TeacherEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Database(
    entities = [
        TeacherEntity::class,
        AttendanceRecordEntity::class,
        SchoolSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun schoolSettingsDao(): SchoolSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bio_attend_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedInitialData(database)
                }
            }
        }
    }
}

suspend fun seedInitialData(database: AppDatabase) {
    val settingsDao = database.schoolSettingsDao()
    val teacherDao = database.teacherDao()
    val attendanceDao = database.attendanceDao()

    // 1. Initial Default School Settings
    settingsDao.insertOrUpdate(
        SchoolSettingsEntity(
            id = 1,
            schoolName = "Greenwood High School & Academy",
            latitude = 37.422065, // Standard Android / emulator coordinates default
            longitude = -122.084064,
            radiusMeters = 250.0,
            shiftStartTime = "08:30",
            shiftEndTime = "15:30",
            lateGraceMinutes = 15,
            earlyGraceMinutes = 10,
            adminPin = "1234",
            simulationBypassEnabled = false
        )
    )

    // 2. Initial Teachers
    val teachers = listOf(
        TeacherEntity(
            phoneNumber = "+1 555-0101",
            fullName = "Sarah Jenkins",
            teacherId = "TCH-101",
            department = "Mathematics"
        ),
        TeacherEntity(
            phoneNumber = "+1 555-0102",
            fullName = "Marcus Vance",
            teacherId = "TCH-102",
            department = "Physics & Chemistry"
        ),
        TeacherEntity(
            phoneNumber = "+1 555-0103",
            fullName = "Elena Rostova",
            teacherId = "TCH-103",
            department = "English Literature"
        ),
        TeacherEntity(
            phoneNumber = "+1 555-0104",
            fullName = "David Chen",
            teacherId = "TCH-104",
            department = "Computer Science"
        ),
        TeacherEntity(
            phoneNumber = "+1 555-0105",
            fullName = "Amanda Lewis",
            teacherId = "TCH-105",
            department = "History & Social Studies"
        )
    )

    val teacherIds = teachers.map { teacherDao.insertTeacher(it) }

    // 3. Seed Past Attendance for realistic Monthly Analytics
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()

    // Seed previous 10 days
    for (dayOffset in 1..10) {
        val pastCal = Calendar.getInstance()
        pastCal.add(Calendar.DAY_OF_YEAR, -dayOffset)
        // Skip Sundays (Calendar.SUNDAY = 1)
        if (pastCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue

        val dateStr = dateFormat.format(pastCal.time)

        teacherIds.forEachIndexed { index, tId ->
            // Simulate realistic attendance
            val isAbsent = (dayOffset == 4 && index == 2) || (dayOffset == 8 && index == 4)
            if (isAbsent) return@forEachIndexed

            val isLate = (dayOffset % 3 == 0 && index % 2 == 1)
            val isEarlyExit = (dayOffset % 4 == 0 && index == 0)

            val inCal = pastCal.clone() as Calendar
            if (isLate) {
                inCal.set(Calendar.HOUR_OF_DAY, 8)
                inCal.set(Calendar.MINUTE, 52) // 22 mins after grace period
            } else {
                inCal.set(Calendar.HOUR_OF_DAY, 8)
                inCal.set(Calendar.MINUTE, 15 + (index * 2))
            }
            val punchInTime = inCal.timeInMillis

            val outCal = pastCal.clone() as Calendar
            if (isEarlyExit) {
                outCal.set(Calendar.HOUR_OF_DAY, 14)
                outCal.set(Calendar.MINUTE, 45) // Left 45 mins early
            } else {
                outCal.set(Calendar.HOUR_OF_DAY, 15)
                outCal.set(Calendar.MINUTE, 35 + (index * 3))
            }
            val punchOutTime = outCal.timeInMillis

            val minutesWorked = ((punchOutTime - punchInTime) / 60000).toInt()

            attendanceDao.insert(
                AttendanceRecordEntity(
                    teacherId = tId,
                    teacherName = teachers[index].fullName,
                    teacherPhone = teachers[index].phoneNumber,
                    date = dateStr,
                    punchInTimestamp = punchInTime,
                    punchOutTimestamp = punchOutTime,
                    punchInLatitude = 37.422065,
                    punchInLongitude = -122.084064,
                    punchOutLatitude = 37.422065,
                    punchOutLongitude = -122.084064,
                    punchInStatus = if (isLate) "LATE" else "ON_TIME",
                    punchOutStatus = if (isEarlyExit) "EARLY_EXIT" else "ON_TIME",
                    minutesLate = if (isLate) 22 else 0,
                    minutesEarly = if (isEarlyExit) 45 else 0,
                    totalMinutesWorked = minutesWorked,
                    isGeofenceVerified = true
                )
            )
        }
    }
}
