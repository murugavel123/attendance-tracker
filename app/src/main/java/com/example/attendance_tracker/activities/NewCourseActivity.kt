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
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.attendance_tracker.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay


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

        findViewById<Button>(R.id.btnShowExcelFormat).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excel Format Guide")
                .setMessage("Ensure the Excel file follows this format:\n\n" +
                        "• 1st Column: Roll Numbers\n" +
                        "• 2nd Column: Names\n\n" +
                        "⚠ Do not leave any empty cells in these columns.\n" +
                        "⚠ The file should NOT have headers.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }


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
            val loadingDialog = LoadingDialog(this)

            // Validation before proceeding
            val courseName = editCourseName.text.toString().trim()
            val semesterText = editSemester.text.toString().trim()
            val totalHoursText = editTotalHours.text.toString().trim()
            val selectedYear = spinnerYear.selectedItem.toString().trim()

            // Course Name Validation
            if (courseName.isEmpty()) {
                Toast.makeText(this, "Course Name cannot be empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Semester Validation
            val semesterNo = semesterText.toIntOrNull()
            if (semesterNo == null || semesterNo <= 0) {
                Toast.makeText(this, "Please enter a valid Semester Number!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Year Selection Validation
            if (selectedYear.isEmpty() || selectedYear == "Select Year") {
                Toast.makeText(this, "Please select a valid Year!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Date Selection Validation
            if (selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
                Toast.makeText(this, "Please select both Start and End Dates!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ensure End Date is after Start Date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = dateFormat.parse(selectedStartDate)
            val endDate = dateFormat.parse(selectedEndDate)

            if (endDate.before(startDate)) {
                Toast.makeText(this, "End Date must be after Start Date!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Total Weekly Hours Validation
            val totalHours = totalHoursText.toIntOrNull()
            if (totalHours == null || totalHours <= 0) {
                Toast.makeText(this, "Please enter a valid Total Weekly Hours!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // At least one day must be selected with valid hours
            var assignedHours = 0
            var atLeastOneDaySelected = false

            for (i in dayCheckboxes.indices) {
                if (dayCheckboxes[i].isChecked) {
                    atLeastOneDaySelected = true
                    val hours = hourInputs[i].text.toString().toIntOrNull() ?: 0
                    if (hours <= 0) {
                        Toast.makeText(this, "Enter valid hours for selected days!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    assignedHours += hours
                }
            }

            if (!atLeastOneDaySelected) {
                Toast.makeText(this, "Please select at least one day and assign hours!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Assigned Hours Validation
            if (assignedHours != totalHours) {
                Toast.makeText(this, "Assigned hours must match Total Weekly Hours!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Excel File Import Validation
            if (!isExcelImported) {
                Toast.makeText(this, "Please import an Excel file before saving!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Excel Format Validation
            if (!isExcelValid) {
                Toast.makeText(this, "Excel format incorrect! Ensure Roll No and Name columns exist.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If all validations pass, proceed with saving the course
            loadingDialog.show()

            CoroutineScope(Dispatchers.IO).launch {
                val course = Course(
                    courseName = courseName,
                    year = selectedYear.toInt(),
                    semesterNo = semesterNo,
                    startDate = selectedStartDate,
                    endDate = selectedEndDate,
                    hoursPerWeek = totalHours
                )
                val courseId = courseDao.insertCourse(course).toInt()

                val calendar = Calendar.getInstance()
                calendar.time = startDate

                while (!calendar.time.after(endDate)) {
                    val date = dateFormat.format(calendar.time)
                    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

                    val index = dayCheckboxes.indexOfFirst { it.text.toString().equals(dayName, ignoreCase = true) }

                    if (index != -1 && dayCheckboxes[index].isChecked) {
                        val hoursForTheDay = hourInputs[index].text.toString().toIntOrNull() ?: 0

                        val dayId = courseDayDao.insertCourseDay(
                            CourseDay(
                                courseId = courseId,
                                dayName = dayName,
                                date = date,
                                hours = hoursForTheDay,
                                isHoliday = false,
                                remark = null,
                                attendanceTaken = false
                            )
                        ).toInt()

                        Log.d("Attendance", "Added CourseDay: ID=$dayId, Date=$date, Hours=$hoursForTheDay")

                        for ((rollNo, name) in studentList) {
                            studentDao.insertStudent(
                                Student(dayId = dayId, rollNo = rollNo, name = name, present = true, courseId = courseId)
                            )
                            Log.d("Attendance", "Assigned Student: $name (Roll No: $rollNo) to Day ID: $dayId")
                        }
                    }

                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NewCourseActivity, "Course, Schedule, and Students Added", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismiss()
                    setResult(RESULT_OK)
                    finish()
                }
            }
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
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show() // Show loader before processing

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0) // Read first sheet

                val tempList = mutableListOf<Pair<String, String>>() // Temporary list

                val firstRow = sheet.getRow(0)
                if (firstRow != null && firstRow.physicalNumberOfCells >= 2) {
                    isExcelImported = true
                    isExcelValid = true

                    // Iterate over rows explicitly using sheet.iterator()
                    val rowIterator = sheet.iterator()
                    while (rowIterator.hasNext()) {
                        val row = rowIterator.next()

                        val rollNo = row.getCell(0)?.toString()?.trim() ?: continue
                        val name = row.getCell(1)?.toString()?.trim() ?: continue
                        tempList.add(Pair(rollNo, name)) // Preserve original order
                    }

                    withContext(Dispatchers.Main) {
                        studentList.clear()
                        studentList.addAll(tempList) // Directly update UI list in preserved order
                        findViewById<TextView>(R.id.importView).apply {
                            text = "Successfully imported ${studentList.size} students"
                            setTextColor(Color.parseColor("#388E3C")) // Green color
                        }
                        Toast.makeText(this@NewCourseActivity, "Imported ${studentList.size} students!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isExcelValid = false
                        isExcelImported = false
                        findViewById<TextView>(R.id.importView).apply {
                            text = "Invalid Excel format! Ensure Roll No & Name columns exist."
                            setTextColor(Color.RED)
                        }
                        Toast.makeText(this@NewCourseActivity, "Invalid Excel format!", Toast.LENGTH_SHORT).show()
                    }
                }

                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isExcelValid = false
                    isExcelImported = false
                    findViewById<TextView>(R.id.importView).apply {
                        text = "Failed to import Excel file!"
                        setTextColor(Color.RED)
                    }
                    Toast.makeText(this@NewCourseActivity, "Failed to import Excel file!", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss() // Hide loader after processing
                }
            }
        }
    }



    private fun showInvalidFormatToast() {
        CoroutineScope(Dispatchers.Main).launch {
            isExcelValid = false
            isExcelImported = false
            findViewById<TextView>(R.id.importView).apply {
                text = "Invalid Excel format! Ensure 'Roll No' & 'Name' columns exist."
                setTextColor(Color.RED)
            }
            Toast.makeText(this@NewCourseActivity, "Invalid Excel format!", Toast.LENGTH_LONG).show()
        }
    }


}
