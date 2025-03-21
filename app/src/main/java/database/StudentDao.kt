package com.example.attendance_tracker.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

class StudentDao(private val db: SQLiteDatabase) {

    fun insertStudent(student: Student): Long {
        val values = ContentValues().apply {
            put("day_id", student.dayId)
            put("roll_no", student.rollNo)
            put("name", student.name)
            put("present", if (student.present) 1 else 0)
            put("course_id", student.courseId)
        }
        return db.insert("Student", null, values)
    }

    fun getStudentsByDayId(dayId: Int): List<Student> {
        val students = mutableListOf<Student>()
        val cursor = db.query(
            "Student",
            null,
            "day_id = ?",
            arrayOf(dayId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                students.add(
                    Student(
                        it.getInt(it.getColumnIndexOrThrow("day_id")),
                        it.getString(it.getColumnIndexOrThrow("roll_no")),
                        it.getString(it.getColumnIndexOrThrow("name")),
                        it.getInt(it.getColumnIndexOrThrow("present")) == 1,
                        it.getInt(it.getColumnIndexOrThrow("course_id"))
                    )
                )
            }
        }
        return students
    }

    fun updateStudent(student: Student): Int {
        val values = ContentValues().apply {
            put("name", student.name)
            put("present", if (student.present) 1 else 0)
        }
        return db.update(
            "Student", values, "day_id = ? AND roll_no = ?",
            arrayOf(student.dayId.toString(), student.rollNo)
        )
    }

    fun deleteStudent(dayId: Int, rollNo: String): Int {
        return db.delete("Student", "day_id = ? AND roll_no = ?", arrayOf(dayId.toString(), rollNo))
    }
}
