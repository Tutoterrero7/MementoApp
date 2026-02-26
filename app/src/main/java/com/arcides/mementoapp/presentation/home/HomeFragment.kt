package com.arcides.mementoapp.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.FragmentHomeBinding
import com.arcides.mementoapp.domain.models.Category
import com.arcides.mementoapp.domain.models.Task
import com.arcides.mementoapp.presentation.GlobalActionProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
class HomeFragment : Fragment(), GlobalActionProvider {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var tasksAdapter: TasksAdapter
    
    private var currentFilterCategoryId: String? = null
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        binding.toolbar.title = "Mis Tareas"
    }

    private fun setupUI() {
        // Inflar el menú que contiene el botón de categorías
        binding.toolbar.inflateMenu(R.menu.home_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_categories -> {
                    // Navegar a la pantalla de categorías
                    findNavController().navigate(R.id.action_homeFragment_to_categoriesFragment)
                    true
                }
                else -> false
            }
        }

        binding.btnFilterCategory.setOnClickListener {
            showFilterCategorySelectionDialog()
        }

        binding.statusChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val status = when (checkedId) {
                R.id.chipPending -> Task.TaskStatus.PENDING
                R.id.chipCompleted -> Task.TaskStatus.COMPLETED
                else -> null
            }
            viewModel.setStatusFilter(status)
        }

        binding.priorityChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val priority = when (checkedId) {
                R.id.chipHigh -> Task.Priority.HIGH
                R.id.chipMedium -> Task.Priority.MEDIUM
                R.id.chipLow -> Task.Priority.LOW
                else -> null
            }
            viewModel.setPriorityFilter(priority)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText)
                return true
            }
        })

        tasksAdapter = TasksAdapter(
            onStatusChange = { taskId, newStatus ->
                viewModel.updateTaskStatus(taskId, newStatus)
            },
            onTaskEdit = { task ->
                showEditTaskDialog(task)
            },
            onTaskDeleted = { taskId ->
                showDeleteConfirmation(taskId)
            }
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        when (state) {
                            is HomeViewModel.HomeUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is HomeViewModel.HomeUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                tasksAdapter.submitList(state.tasks)
                                
                                if (state.tasks.isEmpty()) {
                                    binding.tasksRecyclerView.visibility = View.GONE
                                } else {
                                    binding.tasksRecyclerView.visibility = View.VISIBLE
                                }
                            }
                            is HomeViewModel.HomeUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                showSnackbar(state.message)
                            }
                        }
                    }
                }

                launch {
                    viewModel.message.collectLatest { message ->
                        message?.let {
                            showSnackbar(it)
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        // Obtenemos el FAB de la Activity para usarlo como ancla
        val fab = requireActivity().findViewById<View>(R.id.fab_add)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).apply {
            // Si el FAB es visible, lo usamos como ancla para que el Snackbar aparezca encima de él
            if (fab != null && fab.visibility == View.VISIBLE) {
                anchorView = fab
            }
            show()
        }
    }

    private fun showFilterCategorySelectionDialog() {
        val categories = viewModel.categories.value
        val categoryNames = mutableListOf("Todas las categorías")
        categoryNames.addAll(categories.map { it.name })
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtrar por categoría")
            .setItems(categoryNames.toTypedArray()) { _, which ->
                if (which == 0) {
                    currentFilterCategoryId = null
                    binding.btnFilterCategory.text = "Categoría"
                } else {
                    val selectedCategory = categories[which - 1]
                    currentFilterCategoryId = selectedCategory.id
                    binding.btnFilterCategory.text = selectedCategory.name
                }
                viewModel.setCategoryFilter(currentFilterCategoryId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onPrimaryActionClicked() {
        showAddTaskDialog()
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_task, null)
        
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val priorityGroup = dialogView.findViewById<RadioGroup>(R.id.priorityGroup)
        val categoryButton = dialogView.findViewById<Button>(R.id.categoryButton)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        
        var selectedCategoryId = ""
        var selectedPriority = Task.Priority.MEDIUM
        val calendar = Calendar.getInstance()
        var selectedDate: Date? = null
        
        dateInput.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
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
                
                selectedDate = calendar.time
                timeInput.setText(timeFormatter.format(selectedDate!!))
            }
            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }
        
        categoryButton.setOnClickListener {
            showCategorySelectionDialog(selectedCategoryId) { category ->
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
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Nueva Tarea")
            .setPositiveButton("Crear") { _, _ ->
                val title = titleInput.text?.toString()?.trim()
                val description = descriptionInput.text?.toString()?.trim()
                
                if (!title.isNullOrEmpty()) {
                    viewModel.createTask(
                        title = title,
                        description = description ?: "",
                        priority = selectedPriority,
                        categoryId = selectedCategoryId,
                        dueDate = selectedDate
                    )
                } else {
                    showSnackbar("El título es requerido")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_task, null)
        
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val priorityGroup = dialogView.findViewById<RadioGroup>(R.id.priorityGroup)
        val categoryButton = dialogView.findViewById<Button>(R.id.categoryButton)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        
        titleInput.setText(task.title)
        descriptionInput.setText(task.description)
        
        var selectedCategoryId = task.categoryId
        var selectedPriority = task.priority
        val calendar = Calendar.getInstance()
        var selectedDate: Date? = task.dueDate
        
        task.dueDate?.let {
            calendar.time = it
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
                
                selectedDate = calendar.time
                timeInput.setText(timeFormatter.format(selectedDate!!))
            }
            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }
        
        when (task.priority) {
            Task.Priority.LOW -> priorityGroup.check(R.id.priorityLow)
            Task.Priority.HIGH -> priorityGroup.check(R.id.priorityHigh)
            Task.Priority.MEDIUM -> priorityGroup.check(R.id.priorityMedium)
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
        
        priorityGroup.setOnCheckedChangeListener { _, checkedId ->
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
                            dueDate = selectedDate
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
            showSnackbar("No hay categorías creadas")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
