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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var tasksAdapter: TasksAdapter
    
    private var currentFilterCategoryId: String? = null

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
        binding.toolbar.inflateMenu(R.menu.home_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_categories -> {
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
                // Usamos la función del ViewModel para actualizar el estado específico
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

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
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
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.message.collectLatest { message ->
                        message?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearMessage()
                        }
                    }
                }
            }
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

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_task, null)
        
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val priorityGroup = dialogView.findViewById<RadioGroup>(R.id.priorityGroup)
        val categoryButton = dialogView.findViewById<Button>(R.id.categoryButton)
        
        var selectedCategoryId = ""
        var selectedPriority = Task.Priority.MEDIUM
        
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
                        categoryId = selectedCategoryId
                    )
                } else {
                    Snackbar.make(binding.root, "El título es requerido", Snackbar.LENGTH_SHORT).show()
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
        
        titleInput.setText(task.title)
        descriptionInput.setText(task.description)
        
        var selectedCategoryId = task.categoryId
        var selectedPriority = task.priority
        
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
                            categoryId = selectedCategoryId
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
            Snackbar.make(binding.root, "No hay categorías creadas", Snackbar.LENGTH_SHORT).show()
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
