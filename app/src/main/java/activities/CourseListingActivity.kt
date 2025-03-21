package com.example.attendance_tracker.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.attendance_tracker.R
import com.example.attendance_tracker.adapters.CourseAdapter
import com.example.attendance_tracker.database.CourseDao
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CourseListingActivity : AppCompatActivity() {
    private lateinit var courseDao: CourseDao
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textNoCourses: TextView
    private lateinit var fabAddCourse: FloatingActionButton
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_listing)

        courseDao = CourseDao(this)
        recyclerView = findViewById(R.id.recyclerView)
        textNoCourses = findViewById(R.id.textNoCourses)
        fabAddCourse = findViewById(R.id.fabAddCourse)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Register the ActivityResultLauncher
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Refresh RecyclerView when coming back from NewCourseActivity
                loadCourses()
            }
        }

        loadCourses()

        fabAddCourse.setOnClickListener {
            val intent = Intent(this, NewCourseActivity::class.java)
            activityResultLauncher.launch(intent)
        }
    }

    private fun loadCourses() {
        val courses = courseDao.getAllCourses()
        courseAdapter = CourseAdapter(courses) { courseId ->
            val intent = Intent(this, ClassesListingActivity::class.java)
            intent.putExtra("courseId", courseId)
            startActivity(intent)
        }
        recyclerView.adapter = courseAdapter
        textNoCourses.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
    }
}


