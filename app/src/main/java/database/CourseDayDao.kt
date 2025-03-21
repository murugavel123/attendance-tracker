package com.example.attendance_tracker.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

class CourseDayDao(private val db: SQLiteDatabase) {

    fun insertCourseDay(courseDay: CourseDay): Long {
        val values = ContentValues().apply {
            put("course_id", courseDay.courseId)
            put("day_name", courseDay.dayName)
            put("date", courseDay.date)
            put("hours", courseDay.hours)
            put("attendance_taken", if (courseDay.attendanceTaken) 1 else 0)
        }
        return db.insert("CourseDay", null, values)
    }

    fun getCourseDaysByCourseId(courseId: Int): List<CourseDay> {
        val classes = mutableListOf<CourseDay>()
        val cursor = db.query(
            "CourseDay", null, "course_id = ?", arrayOf(courseId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val dayId = cursor.getInt(cursor.getColumnIndexOrThrow("day_id")) // Fetch dayId
                val dayName = cursor.getString(cursor.getColumnIndexOrThrow("day_name"))
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val hours = cursor.getInt(cursor.getColumnIndexOrThrow("hours"))
                val attendanceTaken = cursor.getInt(cursor.getColumnIndexOrThrow("attendance_taken")) == 1

                classes.add(
                    CourseDay(
                        dayId = dayId,
                        courseId = courseId,
                        dayName = dayName,
                        date = date,
                        hours = hours,
                        attendanceTaken = attendanceTaken
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        return classes
    }

}

