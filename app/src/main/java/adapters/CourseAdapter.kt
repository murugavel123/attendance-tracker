package com.example.attendance_tracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance_tracker.R
import com.example.attendance_tracker.database.Course
class CourseAdapter(
    private val courses: List<Course>,
    private val onCourseClick: (Int) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textCourseName: TextView = view.findViewById(R.id.textCourseName)
        val textCourseDate: TextView = view.findViewById(R.id.textCourseDate)

        fun bind(course: Course) {
            textCourseName.text = course.courseName
            textCourseDate.text = "${course.startDate} - ${course.endDate}"

            itemView.setOnClickListener {
                onCourseClick(course.courseId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount() = courses.size
}

