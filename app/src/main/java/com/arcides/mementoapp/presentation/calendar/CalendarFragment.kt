package com.arcides.mementoapp.presentation.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentCalendarBinding
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.presentation.home.HomeViewModel
import com.arcides.mementoapp.presentation.home.TasksAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var tasksAdapter: TasksAdapter
    private var selectedDate: Date = Calendar.getInstance().time
    
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCalendar()
        observeTasks()
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(
            onStatusChange = { id, status -> viewModel.updateTaskStatus(id, status) },
            onTaskEdit = { task -> showEditTaskDialog(task) },
            onTaskDeleted = { id -> showDeleteConfirmation(id) }
        )
        binding.calendarRecyclerView.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_task, null)

        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val priorityGroup = android.widget.RadioGroup(requireContext()) // Fallback si no está en el layout, pero debería estar
        val categoryButton = dialogView.findViewById<android.widget.Button>(R.id.categoryButton)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val actualPriorityGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.priorityGroup)

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)

        var selectedCategoryId = task.categoryId
        var selectedPriority = task.priority
        val calendar = Calendar.getInstance()
        var currentSelectedDate: Date? = task.dueDate

        task.dueDate?.let {
            calendar.time = it
            dateInput.setText(dateFormatter.format(it))
            timeInput.setText(timeFormatter.format(it))
        }

        dateInput.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(currentSelectedDate?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = Date(selection)
                val dateCalendar = Calendar.getInstance()
                dateCalendar.time = date

                calendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))

                currentSelectedDate = calendar.time
                dateInput.setText(dateFormatter.format(currentSelectedDate!!))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
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

                currentSelectedDate = calendar.time
                timeInput.setText(timeFormatter.format(currentSelectedDate!!))
            }
            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }

        when (task.priority) {
            Task.Priority.LOW -> actualPriorityGroup.check(R.id.priorityLow)
            Task.Priority.HIGH -> actualPriorityGroup.check(R.id.priorityHigh)
            Task.Priority.MEDIUM -> actualPriorityGroup.check(R.id.priorityMedium)
        }

        if (selectedCategoryId.isNotBlank()) {
            val category = viewModel.categories.value.find { it.id == selectedCategoryId }
            categoryButton.text = category?.name ?: "Seleccionar categoría"
        }

        categoryButton.setOnClickListener {
            showCategorySelectionDialog(selectedCategoryId) { category ->
                selectedCategoryId = category?.id ?: ""
                categoryButton.text = category?.name ?: "Seleccionar categoría"
            }
        }

        actualPriorityGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPriority = when (checkedId) {
                R.id.priorityLow -> Task.Priority.LOW
                R.id.priorityHigh -> Task.Priority.HIGH
                else -> Task.Priority.MEDIUM
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Editar Tarea")
            .setPositiveButton("Guardar") { _, _ ->
                val title = titleInput.text?.toString()?.trim() ?: ""
                val description = descriptionInput.text?.toString()?.trim() ?: ""

                if (title.isNotEmpty()) {
                    viewModel.updateTask(
                        task.copy(
                            title = title,
                            description = description,
                            priority = selectedPriority,
                            categoryId = selectedCategoryId,
                            dueDate = currentSelectedDate
                        )
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCategorySelectionDialog(
        currentCategoryId: String,
        onCategorySelected: (Category?) -> Unit
    ) {
        val categories = viewModel.categories.value

        if (categories.isEmpty()) {
            onCategorySelected(null)
            return
        }

        val categoryNames = categories.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar categoría")
            .setItems(categoryNames) { _, which ->
                val selectedCategory = categories[which]
                onCategorySelected(selectedCategory)
            }
            .setNeutralButton("Sin categoría") { _, _ ->
                onCategorySelected(null)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(taskId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de eliminar esta tarea?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteTask(taskId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateFilteredTasks()
        }
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    if (state is HomeViewModel.HomeUiState.Success) {
                        updateFilteredTasks(state.tasks)
                    }
                }
            }
        }
    }

    private fun updateFilteredTasks(tasks: List<Task>? = null) {
        val currentTasks = tasks ?: (viewModel.uiState.value as? HomeViewModel.HomeUiState.Success)?.tasks ?: emptyList()
        val filtered = currentTasks.filter { task ->
            task.dueDate?.let { isSameDay(it, selectedDate) } ?: false
        }
        
        tasksAdapter.submitList(filtered)
        binding.emptyCalendarText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.calendarRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}