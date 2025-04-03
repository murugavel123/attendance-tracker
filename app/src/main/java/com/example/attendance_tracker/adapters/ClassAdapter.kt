package com.example.attendance_tracker.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance_tracker.R
import com.example.attendance_tracker.database.CourseDay
import android.graphics.Color
import com.example.attendance_tracker.activities.AttendanceStudentListing
import android.content.Intent
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.attendance_tracker.database.AppDatabase
import com.example.attendance_tracker.database.CourseDayDao
class ClassAdapter(
    private var classes: MutableList<CourseDay>
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDayName: TextView = view.findViewById(R.id.textDayName)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textHours: TextView = view.findViewById(R.id.textHours)
        val cardView: CardView = view as CardView
        val btnView: Button = view.findViewById(R.id.btnView)
        val btnHoliday: Button = view.findViewById(R.id.btnHoliday)

        fun bind(courseDay: CourseDay) {
            textDayName.text = courseDay.dayName
            textDate.text = courseDay.date
            textHours.text = "Hours: ${courseDay.hours}"

            if (courseDay.isHoliday) {
                cardView.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                btnHoliday.text = "Remove Holiday"
            } else {
                val cardColor = if (courseDay.attendanceTaken) {
                    Color.parseColor("#C8E6C9")
                } else {
                    Color.parseColor("#FFF59D")
                }
                cardView.setCardBackgroundColor(cardColor)
                btnHoliday.text = "Mark as Holiday"
            }

            // View button logic
            btnView.setOnClickListener {
                if (courseDay.isHoliday) {
                    showRemarkDialog(it.context, courseDay.remark ?: "No remark provided")
                } else {
                    val intent = Intent(it.context, AttendanceStudentListing::class.java)
                    intent.putExtra("dayId", courseDay.dayId)
                    intent.putExtra("courseId", courseDay.courseId)
                    it.context.startActivity(intent)
                }
            }

            btnHoliday.setOnClickListener {
                showHolidayDialog(it.context, courseDay, adapterPosition)
            }
        }

        private fun showHolidayDialog(context: Context, courseDay: CourseDay, position: Int) {
            val builder = AlertDialog.Builder(context)

            if (courseDay.isHoliday) {
                // Show confirmation dialog when revoking holiday
                builder.setTitle("Remove Holiday")
                    .setMessage("Are you sure you want to remove this holiday?")
                    .setPositiveButton("Yes") { _, _ ->
                        val database = AppDatabase(context).writableDatabase
                        val courseDayDao = CourseDayDao(database)
                        courseDayDao.setHolidayStatus(courseDay.dayId, false, null)

                        // Update UI and notify adapter
                        classes[position] = classes[position].copy(isHoliday = false, remark = null)
                        notifyItemChanged(position)
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                // Show input dialog when marking as holiday
                val input = EditText(context)
                input.hint = "Enter holiday remark"
                input.inputType = InputType.TYPE_CLASS_TEXT

                builder.setTitle("Mark as Holiday")
                    .setView(input)
                    .setPositiveButton("Mark") { _, _ ->
                        val remark = input.text.toString().trim()
                        val database = AppDatabase(context).writableDatabase
                        val courseDayDao = CourseDayDao(database)
                        courseDayDao.setHolidayStatus(courseDay.dayId, true, remark)

                        // Update UI and notify adapter
                        classes[position] = classes[position].copy(isHoliday = true, remark = remark)
                        notifyItemChanged(position)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }


    }
    private fun showRemarkDialog(context: Context, remark: String) {
        AlertDialog.Builder(context)
            .setTitle("Holiday Remark")
            .setMessage(remark)
            .setPositiveButton("OK", null)
            .show()
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

    fun updateClasses(newClasses: List<CourseDay>) {
        classes.clear()
        classes.addAll(newClasses)
        notifyDataSetChanged()
    }
}



