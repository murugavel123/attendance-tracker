package com.example.attendance_tracker.activities

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance_tracker.R
import com.example.attendance_tracker.adapters.StudentAdapter
import com.example.attendance_tracker.database.AppDatabase
import com.example.attendance_tracker.database.Student
import com.example.attendance_tracker.database.StudentDao

class AttendanceNotTakenStudentListing : AppCompatActivity() {

    private lateinit var studentDao: StudentDao
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var recyclerView: RecyclerView
    private var dayId: Int = 0
    private var courseId: Int = 0
    private lateinit var confirmButton: Button
    private lateinit var students: List<Student>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_listing)

        recyclerView = findViewById(R.id.recyclerView)
        confirmButton = findViewById(R.id.confirmButton)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val db = AppDatabase(this).readableDatabase
        studentDao = StudentDao(db)

        dayId = intent.getIntExtra("dayId", -1)
        courseId = intent.getIntExtra("courseId", -1)

        if (dayId != -1 && courseId != -1) {
            loadStudents()
        }

        confirmButton.setOnClickListener {
            showAbsentStudentsDialog()
        }
    }

    private fun loadStudents() {
        students = studentDao.getStudentsByDayId(dayId)
        studentAdapter = StudentAdapter(this, students)
        recyclerView.adapter = studentAdapter
    }

    private fun showAbsentStudentsDialog() {
        val absentStudents = students.filter { !it.present }
        val absentList = absentStudents.joinToString("\n") { it.name }

        AlertDialog.Builder(this)
            .setTitle("Absent Students")
            .setMessage(absentList.ifEmpty { "No students are absent." })
            .setPositiveButton("OK") { _, _ -> finish() }
            .create()
            .show()
    }
}
