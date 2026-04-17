package com.arcides.mementoapp.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioGroup
import androidx.fragment.app.FragmentManager
import com.arcides.mementoapp.R
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

object TaskDialogHelper {

    fun showAddTaskDialog(
        context: Context,
        fragmentManager: FragmentManager,
        dateFormatter: SimpleDateFormat,
        timeFormatter: SimpleDateFormat,
        initialDate: Date? = null,
        onCategorySelectRequested: (String, (Category?) -> Unit) -> Unit,
        onTaskCreated: (String, String, Task.Priority, String, Date?) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_task, null)

        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val priorityGroup = dialogView.findViewById<RadioGroup>(R.id.priorityGroup)
        val categoryButton = dialogView.findViewById<Button>(R.id.categoryButton)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)

        var selectedCategoryId = ""
        var selectedPriority = Task.Priority.MEDIUM
        val calendar = Calendar.getInstance()
        initialDate?.let { calendar.time = it }
        var selectedDate: Date? = initialDate

        initialDate?.let {
            dateInput.setText(dateFormatter.format(it))
            timeInput.setText(timeFormatter.format(it))
        }

        dateInput.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(selectedDate?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = Date(selection)
                val dateCalendar = Calendar.getInstance()
                dateCalendar.time = date

                calendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))

                selectedDate = calendar.time
                dateInput.setText(dateFormatter.format(selectedDate!!))
            }
            datePicker.show(fragmentManager, "DATE_PICKER")
        }

        timeInput.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Seleccionar hora")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)

                selectedDate = calendar.time
                timeInput.setText(timeFormatter.format(selectedDate!!))
            }
            timePicker.show(fragmentManager, "TIME_PICKER")
        }

        categoryButton.setOnClickListener {
            onCategorySelectRequested(selectedCategoryId) { category ->
                selectedCategoryId = category?.id ?: ""
                categoryButton.text = category?.name ?: "Seleccionar categoría"
            }
        }

        priorityGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPriority = when (checkedId) {
                R.id.priorityLow -> Task.Priority.LOW
                R.id.priorityHigh -> Task.Priority.HIGH
                else -> Task.Priority.MEDIUM
            }
        }

        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setTitle("Nueva Tarea")
            .setPositiveButton("Crear") { _, _ ->
                val title = titleInput.text?.toString()?.trim()
                val description = descriptionInput.text?.toString()?.trim()

                if (!title.isNullOrEmpty()) {
                    onTaskCreated(
                        title,
                        description ?: "",
                        selectedPriority,
                        selectedCategoryId,
                        selectedDate
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}