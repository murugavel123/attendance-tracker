package com.example.attendance_tracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance_tracker.R
import com.example.attendance_tracker.database.CourseDay
import android.graphics.Color
import com.example.attendance_tracker.activities.AttendanceNotTakenStudentListing
import android.content.Intent

class ClassAdapter(
    private val classes: List<CourseDay>
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDayName: TextView = view.findViewById(R.id.textDayName)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textHours: TextView = view.findViewById(R.id.textHours)
        val cardView: CardView = view as CardView
        val btnView: Button = view.findViewById(R.id.btnView)

        fun bind(courseDay: CourseDay) {
            textDayName.text = courseDay.dayName
            textDate.text = courseDay.date
            textHours.text = "Hours: ${courseDay.hours}"

            // Change card color based on attendanceTaken status
            val cardColor = if (courseDay.attendanceTaken) {
                Color.parseColor("#C8E6C9") // Light Green (If attendance taken)
            } else {
                Color.parseColor("#FFCDD2") // Light Red (If attendance not taken)
            }
            cardView.setCardBackgroundColor(cardColor)

            // Handle View Button Click
            btnView.setOnClickListener {
                if (!courseDay.attendanceTaken) {
                    val intent = Intent(it.context, AttendanceNotTakenStudentListing::class.java)
                    intent.putExtra("dayId", courseDay.dayId)
                    intent.putExtra("courseId", courseDay.courseId)
                    it.context.startActivity(intent)
                } else {
                    Toast.makeText(it.context, "Attendance already taken!", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classes[position])
    }

    override fun getItemCount() = classes.size
}


