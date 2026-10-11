package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.TeacherEntity
import com.example.location.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BioAttend", appName)
    }

    @Test
    fun `geofence calculation inside perimeter`() {
        val schoolLat = 37.422065
        val schoolLng = -122.084064
        val radius = 250.0

        // Inside coordinates (very close, e.g. identical or few meters)
        val status = LocationHelper.evaluateGeofence(
            currentLat = 37.422070,
            currentLng = -122.084070,
            schoolLat = schoolLat,
            schoolLng = schoolLng,
            radiusMeters = radius,
            hasPermission = true
        )

        assertTrue("Expected to be inside geofence", status.isInside)
        assertTrue(status.distanceMeters < radius.toFloat())
    }

    @Test
    fun `geofence calculation outside perimeter blocks attendance`() {
        val schoolLat = 37.422065
        val schoolLng = -122.084064
        val radius = 200.0

        // Coordinates ~1.5 km away
        val status = LocationHelper.evaluateGeofence(
            currentLat = 37.435000,
            currentLng = -122.095000,
            schoolLat = schoolLat,
            schoolLng = schoolLng,
            radiusMeters = radius,
            hasPermission = true
        )

        assertFalse("Expected to be outside geofence", status.isInside)
        assertTrue(status.distanceMeters > radius.toFloat())
    }

    @Test
    fun `teacher entity creation and data model test`() {
        val teacher = TeacherEntity(
            id = 1,
            phoneNumber = "+1 555-0101",
            fullName = "Sarah Jenkins",
            teacherId = "TCH-101",
            department = "Mathematics"
        )
        assertEquals("Sarah Jenkins", teacher.fullName)
        assertEquals("+1 555-0101", teacher.phoneNumber)
        assertEquals("Mathematics", teacher.department)
    }

    @Test
    fun `attendance record minutes and status test`() {
        val record = AttendanceRecordEntity(
            id = 10,
            teacherId = 1,
            teacherName = "Sarah Jenkins",
            teacherPhone = "+1 555-0101",
            date = "2026-10-11",
            punchInTimestamp = 1000000L,
            punchOutTimestamp = 1000000L + (7 * 3600 * 1000L),
            punchInLatitude = 37.422065,
            punchInLongitude = -122.084064,
            punchOutLatitude = 37.422065,
            punchOutLongitude = -122.084064,
            punchInStatus = "ON_TIME",
            punchOutStatus = "ON_TIME",
            minutesLate = 0,
            minutesEarly = 0,
            totalMinutesWorked = 420,
            isGeofenceVerified = true
        )

        assertEquals("ON_TIME", record.punchInStatus)
        assertEquals("ON_TIME", record.punchOutStatus)
        assertEquals(420, record.totalMinutesWorked)
        assertTrue(record.isGeofenceVerified)
    }
}
