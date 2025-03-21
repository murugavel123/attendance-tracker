package com.example.attendance_tracker.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance_tracker.R
import com.example.attendance_tracker.database.Student
import android.graphics.Color

class StudentAdapter(
    private val context: Context,
    private val students: List<Student>
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    inner class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val studentCard: CardView = view.findViewById(R.id.studentCard)
        val textName: TextView = view.findViewById(R.id.textName)
        val toggleButton: ToggleButton = view.findViewById(R.id.toggleButton)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.textName.text = student.name
        holder.toggleButton.isChecked = student.present
        updateCardColor(holder, student.present)

        holder.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            student.present = isChecked
            updateCardColor(holder, isChecked)
        }
    }

    private fun updateCardColor(holder: StudentViewHolder, isPresent: Boolean) {
        if (isPresent) {
            holder.studentCard.setCardBackgroundColor(Color.parseColor("#C8E6C9")) // Green
        } else {
            holder.studentCard.setCardBackgroundColor(Color.parseColor("#FFCDD2")) // Red
        }
    }

    override fun getItemCount() = students.size
}
