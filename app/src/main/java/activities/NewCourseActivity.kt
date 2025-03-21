package com.example.attendance_tracker.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.attendance_tracker.R
import com.example.attendance_tracker.database.Course
import com.example.attendance_tracker.database.CourseDay
import com.example.attendance_tracker.database.Student
import com.example.attendance_tracker.database.CourseDao
import com.example.attendance_tracker.database.CourseDayDao
import com.example.attendance_tracker.database.StudentDao
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import android.content.Intent
import android.net.Uri
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentResolver
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.attendance_tracker.database.AppDatabase


class NewCourseActivity : AppCompatActivity() {
    private lateinit var courseDao: CourseDao
    private lateinit var courseDayDao: CourseDayDao
    private lateinit var studentDao: StudentDao
    private lateinit var editCourseName: EditText
    private lateinit var spinnerYear: Spinner
    private lateinit var editSemester: EditText
    private lateinit var textStartDate: TextView
    private lateinit var textEndDate: TextView
    private lateinit var editTotalHours: EditText
    private lateinit var btnSave: Button
    private lateinit var btnImportExcel: Button
    var isExcelImported = false
    var isExcelValid = false
    private val PICK_EXCEL_REQUEST = 100
    private val studentList = mutableListOf<Pair<String, String>>()
    private lateinit var dayCheckboxes: List<CheckBox>
    private lateinit var hourInputs: List<EditText>

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedStartDate: String = ""
    private var selectedEndDate: String = ""
    private val excelPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { readExcelFile(it) }
        }
    private lateinit var dbHelper: AppDatabase
    private lateinit var db: SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_course)
        val dbHelper = AppDatabase(this)
        val db = dbHelper.writableDatabase
        courseDao = CourseDao(this)
        courseDayDao = CourseDayDao(db)
        studentDao = StudentDao(db)
        editCourseName = findViewById(R.id.editCourseName)
        spinnerYear = findViewById(R.id.spinnerYear)
        editSemester = findViewById(R.id.editSemester)
        textStartDate = findViewById(R.id.textStartDate)
        textEndDate = findViewById(R.id.textEndDate)
        editTotalHours = findViewById(R.id.editTotalHours)
        btnSave = findViewById(R.id.btnSave)

        dayCheckboxes = listOf(
            findViewById(R.id.checkMonday),
            findViewById(R.id.checkTuesday),
            findViewById(R.id.checkWednesday),
            findViewById(R.id.checkThursday),
            findViewById(R.id.checkFriday),
            findViewById(R.id.checkSaturday),
            findViewById(R.id.checkSunday)
        )

        hourInputs = listOf(
            findViewById(R.id.editMondayHours),
            findViewById(R.id.editTuesdayHours),
            findViewById(R.id.editWednesdayHours),
            findViewById(R.id.editThursdayHours),
            findViewById(R.id.editFridayHours),
            findViewById(R.id.editSaturdayHours),
            findViewById(R.id.editSundayHours),
        )

        for (i in dayCheckboxes.indices) {
            dayCheckboxes[i].setOnCheckedChangeListener { _, isChecked ->
                hourInputs[i].isEnabled = isChecked
                if (!isChecked) hourInputs[i].setText("")
            }
        }

        textStartDate.setOnClickListener { showDatePickerDialog(true) }
        textEndDate.setOnClickListener { showDatePickerDialog(false) }

        btnImportExcel = findViewById(R.id.btnImportExcel)
        btnImportExcel.setOnClickListener {
            selectExcelFile()
        }

        btnSave.setOnClickListener {
            val totalHours = editTotalHours.text.toString().toIntOrNull() ?: 0
            var assignedHours = 0

            for (hourInput in hourInputs) {
                val hours = hourInput.text.toString().toIntOrNull() ?: 0
                assignedHours += hours
            }

            if (assignedHours != totalHours) {
                Toast.makeText(this, "Assigned hours do not match the required total hours!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
                Toast.makeText(this, "Please select both Start and End Dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isExcelImported) {
                Toast.makeText(this, "Please import an Excel file before saving.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isExcelValid) {
                Toast.makeText(this, "Excel format incorrect! Ensure Roll No and Name columns exist.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val course = Course(
                courseName = editCourseName.text.toString(),
                year = spinnerYear.selectedItem.toString().toInt(),
                semesterNo = editSemester.text.toString().toInt(),
                startDate = selectedStartDate,
                endDate = selectedEndDate,
                hoursPerWeek = totalHours
            )
            val courseId = courseDao.insertCourse(course).toInt()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(selectedStartDate)!!

            val endDate = dateFormat.parse(selectedEndDate)!!
            while (!calendar.time.after(endDate)) {
                val date = dateFormat.format(calendar.time)
                val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

                // Check if this day's checkbox is selected
                val index = dayCheckboxes.indexOfFirst { it.text.toString().equals(dayName, ignoreCase = true) }

                if (index != -1 && dayCheckboxes[index].isChecked) {
                    val hoursForTheDay = hourInputs[index].text.toString().toIntOrNull() ?: 0

                    // Insert only if the day is selected
                    val dayId = courseDayDao.insertCourseDay(
                        CourseDay(courseId = courseId, dayName = dayName, date = date, hours = hoursForTheDay)
                    ).toInt()

                    Log.d("Attendance", "Added CourseDay: ID=$dayId, Date=$date, Hours=$hoursForTheDay")

                    for ((rollNo, name) in studentList) {
                        studentDao.insertStudent(
                            Student(dayId = dayId, rollNo = rollNo, name = name, present = true, courseId = courseId)
                        )
                        Log.d("Attendance", "Assigned Student: $name (Roll No: $rollNo) to Day ID: $dayId")
                    }
                }

                calendar.add(Calendar.DAY_OF_MONTH, 1) // Move to next date
            }
            Toast.makeText(this, "Course, Schedule, and Students Added", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedYear-${String.format("%02d", selectedMonth + 1)}-${String.format("%02d", selectedDay)}"
                if (isStartDate) {
                    selectedStartDate = selectedDate
                    textStartDate.text = "Start Date: $selectedDate"
                } else {
                    selectedEndDate = selectedDate
                    textEndDate.text = "End Date: $selectedDate"
                }
            },
            year, month, day
        )

        datePickerDialog.show()
    }
    private fun selectExcelFile() {
        isExcelImported = false
        isExcelValid = false
        excelPickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_EXCEL_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                readExcelFile(uri)
            }
        }
    }
    private fun readExcelFile(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)  // Read first sheet

            studentList.clear()  // Clear previous data

            // Validate if the Excel has at least one row and two columns
            if (sheet.physicalNumberOfRows > 0) {
                val firstRow = sheet.getRow(0)
                if (firstRow != null && firstRow.physicalNumberOfCells >= 2) {
                    isExcelImported = true
                    isExcelValid = true  // File is valid

                    for (row in sheet) {
                        val rollNo = row.getCell(0)?.toString()?.trim() ?: continue
                        val name = row.getCell(1)?.toString()?.trim() ?: continue
                        studentList.add(Pair(rollNo, name))
                    }

                    Toast.makeText(this, "Imported ${studentList.size} students!", Toast.LENGTH_SHORT).show()
                } else {
                    isExcelValid = false
                    isExcelImported = false
                    Toast.makeText(this, "Invalid Excel format! Ensure two columns: Roll No & Name.", Toast.LENGTH_SHORT).show()
                }
            } else {
                isExcelValid = false
                isExcelImported = false
                Toast.makeText(this, "Empty Excel file!", Toast.LENGTH_SHORT).show()
            }

            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            isExcelValid = false
            isExcelImported = false
            Toast.makeText(this, "Failed to import Excel file!", Toast.LENGTH_SHORT).show()
        }
    }
}
